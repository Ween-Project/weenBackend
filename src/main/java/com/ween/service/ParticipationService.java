package com.ween.service;

import com.ween.dto.response.CheckinResponse;
import com.ween.entity.*;
import com.ween.enums.ParticipationStatus;
import com.ween.exception.AlreadyExistsException;
import com.ween.exception.EventNotRegisteredException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.*;
import com.ween.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CertificateTriggerService certificateTriggerService;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final NotificationService notificationService;
    private final QrService qrService;
    private final RegistrationService registrationService;
    private final SecurityUtil securityUtil;
    private final CertificateRepository certificateRepository;
    private final OrganizerRepository organizerRepository;


    private void validateUserRegistration(String eventId, String userId) {
        boolean isRegistered = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId).isPresent();
        if (!isRegistered) {
            log.warn("Access denied: User {} tried to access participation for event {} without registration", userId, eventId);
            throw new EventNotRegisteredException("You cannot participate or view details because you are not registered for this event.");
        }
    }

    @Transactional
    public CheckinResponse checkinViaQr(String eventId, String qrToken) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        String currentUserId = securityUtil.getCurrentUserId();
        
        boolean isOwner = event.getOrganizationId().equals(currentUserId);
        boolean isOrganizer = organizerRepository.findByUserId(currentUserId)
                .map(org -> org.getOrganization().getId().equals(event.getOrganizationId()))
                .orElse(false);

        if (!isOwner && !isOrganizer) {
            throw new AccessDeniedException("Only the event owner or organizer can perform check-in");
        }

        String participantUserId = qrService.validateAndDecryptQrToken(qrToken);
        User participant = userRepository.findById(participantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantUserId));

        joinEventInternal(participantUserId, eventId);
        registrationService.markUserAsJoined(eventId, participantUserId);

        String participantName = participant.getFullName() != null && !participant.getFullName().isBlank()
                ? participant.getFullName()
                : participant.getUsername();

        return CheckinResponse.builder()
                .status("CHECKED_IN")
                .participantName(participantName)
                .participantPhoto(participant.getProfilePhotoUrl())
                .message("Check-in successful")
                .build();
    }

    private void joinEventInternal(String userId, String eventId) {
        validateUserRegistration(eventId, userId);

        Participation participation = participationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseGet(() -> {
                    log.info("Participation record missing for registered user. Creating new one. User: {}, Event: {}", userId, eventId);
                    Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
                    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    return participationRepository.save(Participation.builder()
                            .user(user)
                            .event(event)
                            .status(ParticipationStatus.JOINED)
                            .joinedAt(LocalDateTime.now())
                            .build());
                });

        if (participation.getStatus() == ParticipationStatus.APPROVED) {
            throw new AlreadyExistsException("User has already checked in to this event");
        }

        participation.setStatus(ParticipationStatus.APPROVED);
        participationRepository.save(participation);
        notificationService.createAttendanceConfirmedNotification(userId, eventId);
    }




    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void scheduleEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusHours(24);

        List<Event> upcomingEvents = eventRepository.findEventsStartingBetween(
                tomorrow.minusMinutes(30),
                tomorrow.plusMinutes(30),
                com.ween.enums.EventStatus.PUBLISHED
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        for (Event event : upcomingEvents) {
            String timeStr = event.getStartDate().format(formatter);
            List<EventRegistration> registrations = eventRegistrationRepository.findByEventId(event.getId());

            for (EventRegistration reg : registrations) {
                try {
                    notificationService.createEventReminderNotification(reg.getUserId(), event.getId(), timeStr);
                } catch (Exception e) {
                    log.error("Failed to send reminder for user {} event {}", reg.getUserId(), event.getId(), e);
                }
            }
        }
    }

    @Transactional
    public Certificate completeParticipationAndGetCertificate(String userId, String eventId) {
        Participation participation = participationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseGet(() -> {
                    log.info("Participation record missing for registered user. Creating new one. User: {}, Event: {}", userId, eventId);
                    User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found"));
                    return participationRepository.save(Participation.builder()
                            .user(user)
                            .event(event)
                            .status(ParticipationStatus.APPROVED)
                            .joinedAt(LocalDateTime.now())
                            .build());
                });

        participation.setStatus(ParticipationStatus.FINISHED);
        participationRepository.save(participation);

        log.info("Participation completed. Auto-generating certificate record for User: {}, Event: {}", userId, eventId);

        return certificateTriggerService.autoGenerateCertificateRecord(
                userId,
                eventId,
                participation.getEvent().getCategory()
        );
    }

    @Transactional
    public void saveCertificateUrl(String certificateId, String pdfUrl) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found. ID: " + certificateId));
        certificate.setPdfUrl(pdfUrl);
        certificateRepository.save(certificate);

        notificationService.createCertificateNotification(
                certificate.getUserId(),
                certificate.getEventId(),
                certificate.getCertificateNumber()
        );
    }
}
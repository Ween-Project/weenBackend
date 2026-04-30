package com.ween.service;

import com.ween.entity.*;
import com.ween.enums.ParticipationStatus;
import com.ween.exception.EventNotRegisteredException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CertificateTriggerService certificateTriggerService;
    private final EventRegistrationRepository eventRegistrationRepository;

    private void validateUserRegistration(String eventId, String userId) {
        boolean isRegistered = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId).isPresent();
        if (!isRegistered) {
            log.warn("Access denied: User {} tried to access participation for event {} without registration", userId, eventId);
            throw new EventNotRegisteredException("You cannot participate or view details because you are not registered for this event.");
        }
    }

    @Transactional
    public void joinEvent(String userId, String eventId) {
        validateUserRegistration(eventId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        Participation participation = Participation.builder()
                .user(user)
                .event(event)
                .status(ParticipationStatus.JOINED)
                .joinedAt(LocalDateTime.now())
                .build();

        participationRepository.save(participation);
    }

    @Transactional
    public void completeParticipation(String userId, String eventId) {
        validateUserRegistration(eventId, userId);

        Participation participation = participationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation record not found"));

        participation.setStatus(ParticipationStatus.FINISHED);
        participationRepository.save(participation);

        log.info("Participation completed. Triggering certificate generation for User: {}, Event: {}", userId, eventId);

        certificateTriggerService.autoGenerateCertificateRecord(
                userId,
                eventId,
                participation.getEvent().getCategory()
        );
    }
}
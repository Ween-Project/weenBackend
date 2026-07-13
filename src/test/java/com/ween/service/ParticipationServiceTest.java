package com.ween.service;

import com.ween.dto.response.CheckinResponse;
import com.ween.entity.*;
import com.ween.enums.EventCategory;
import com.ween.enums.ParticipationStatus;
import com.ween.exception.EventNotRegisteredException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.*;
import com.ween.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock private ParticipationRepository participationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private CertificateTriggerService certificateTriggerService;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private NotificationService notificationService;
    @Mock private QrService qrService;
    @Mock private RegistrationService registrationService;
    @Mock private SecurityUtil securityUtil;
    @Mock private CertificateRepository certificateRepository;
    @Mock private OrganizerRepository organizerRepository;

    @InjectMocks
    private ParticipationService participationService;

    @Test
    void checkinViaQrThrowsExceptionWhenNotOwnerOrOrganizer() {
        Event event = mock(Event.class);
        when(event.getOrganizationId()).thenReturn("org-owner-id");
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));

        when(securityUtil.getCurrentUserId()).thenReturn("malicious-user-id");
        when(organizerRepository.findByUserId("malicious-user-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.checkinViaQr("event-123", "qr-token"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the event owner or organizer can perform check-in");
    }

    @Test
    void checkinViaQrThrowsExceptionWhenUserNotRegistered() {
        Event event = mock(Event.class);
        when(event.getOrganizationId()).thenReturn("org-owner-id");
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));

        when(securityUtil.getCurrentUserId()).thenReturn("org-owner-id");
        when(qrService.validateAndDecryptQrToken("qr-token")).thenReturn("participant-id");

        User participant = mock(User.class);
        when(userRepository.findById("participant-id")).thenReturn(Optional.of(participant));

        // Not registered
        when(eventRegistrationRepository.findByEventIdAndUserId("event-123", "participant-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.checkinViaQr("event-123", "qr-token"))
                .isInstanceOf(EventNotRegisteredException.class)
                .hasMessageContaining("You cannot participate or view details because you are not registered");
    }

    @Test
    void checkinViaQrSucceedsForValidRegistration() {
        Event event = mock(Event.class);
        when(event.getOrganizationId()).thenReturn("org-owner-id");
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));

        when(securityUtil.getCurrentUserId()).thenReturn("org-owner-id");
        when(qrService.validateAndDecryptQrToken("qr-token")).thenReturn("participant-id");

        User participant = mock(User.class);
        when(participant.getFullName()).thenReturn("Ali Aliyev");
        when(userRepository.findById("participant-id")).thenReturn(Optional.of(participant));

        EventRegistration registration = mock(EventRegistration.class);
        when(eventRegistrationRepository.findByEventIdAndUserId("event-123", "participant-id"))
                .thenReturn(Optional.of(registration));

        when(participationRepository.findByUserIdAndEventId("participant-id", "event-123"))
                .thenReturn(Optional.of(mock(Participation.class)));

        CheckinResponse response = participationService.checkinViaQr("event-123", "qr-token");

        assertThat(response.getStatus()).isEqualTo("CHECKED_IN");
        assertThat(response.getParticipantName()).isEqualTo("Ali Aliyev");
        verify(registrationService).markUserAsJoined("event-123", "participant-id");
        verify(notificationService).createAttendanceConfirmedNotification("participant-id", "event-123");
    }

    @Test
    void scheduleEventRemindersSendsNotificationToRegisteredUsers() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("event-123");
        when(event.getStartDate()).thenReturn(LocalDateTime.now());
        when(eventRepository.findEventsStartingBetween(any(), any(), any())).thenReturn(List.of(event));

        EventRegistration reg = mock(EventRegistration.class);
        when(reg.getUserId()).thenReturn("user-1");
        when(eventRegistrationRepository.findByEventId("event-123")).thenReturn(List.of(reg));

        participationService.scheduleEventReminders();

        verify(notificationService).createEventReminderNotification(eq("user-1"), eq("event-123"), anyString());
    }

    @Test
    void completeParticipationGeneratesCertificateRecord() {
        Event event = mock(Event.class);
        when(event.getCategory()).thenReturn(EventCategory.EDUCATION);

        Participation participation = mock(Participation.class);
        when(participation.getEvent()).thenReturn(event);
        when(participationRepository.findByUserIdAndEventId("user-1", "event-123"))
                .thenReturn(Optional.of(participation));

        Certificate certificate = mock(Certificate.class);
        when(certificateTriggerService.autoGenerateCertificateRecord("user-1", "event-123", EventCategory.EDUCATION))
                .thenReturn(certificate);

        Certificate result = participationService.completeParticipationAndGetCertificate("user-1", "event-123");

        verify(participation).setStatus(ParticipationStatus.FINISHED);
        verify(participationRepository).save(participation);
        assertThat(result).isEqualTo(certificate);
    }

    @Test
    void saveCertificateUrlUpdatesPdfUrlAndSendsNotification() {
        Certificate certificate = mock(Certificate.class);
        when(certificate.getUserId()).thenReturn("user-1");
        when(certificate.getEventId()).thenReturn("event-123");
        when(certificate.getCertificateNumber()).thenReturn("CERT-123");
        when(certificateRepository.findById("cert-123")).thenReturn(Optional.of(certificate));

        participationService.saveCertificateUrl("cert-123", "http://pdf-url");

        verify(certificate).setPdfUrl("http://pdf-url");
        verify(certificateRepository).save(certificate);
        verify(notificationService).createCertificateNotification("user-1", "event-123", "CERT-123");
    }
}

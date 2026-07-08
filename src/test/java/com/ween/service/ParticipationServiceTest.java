package com.ween.service;

import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.ParticipationRepository;
import com.ween.repository.UserRepository;
import com.ween.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock ParticipationRepository participationRepository;
    @Mock UserRepository userRepository;
    @Mock EventRepository eventRepository;
    @Mock CertificateTriggerService certificateTriggerService;
    @Mock EventRegistrationRepository eventRegistrationRepository;
    @Mock NotificationService notificationService;
    @Mock QrService qrService;
    @Mock RegistrationService registrationService;
    @Mock SecurityUtil securityUtil;
    @InjectMocks ParticipationService participationService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(participationService).isNotNull();
    }
}

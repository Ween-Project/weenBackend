package com.ween.service;

import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock EventRegistrationRepository eventRegistrationRepository;
    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock ChatService chatService;
    @Mock CoinService coinService;
    @InjectMocks RegistrationService registrationService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(registrationService).isNotNull();
    }
}

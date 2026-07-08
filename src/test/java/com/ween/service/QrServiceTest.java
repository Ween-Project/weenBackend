package com.ween.service;

import com.ween.repository.EventRepository;
import com.ween.repository.QrTokenRepository;
import com.ween.repository.UserRepository;
import com.ween.security.AesUtil;
import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class QrServiceTest {

    @Mock QrTokenRepository qrTokenRepository;
    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock JwtUtil jwtUtil;
    @Mock AesUtil aesUtil;
    @Mock SecurityUtil securityUtil;
    @Mock RegistrationService registrationService;
    @InjectMocks QrService qrService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(qrService).isNotNull();
    }
}

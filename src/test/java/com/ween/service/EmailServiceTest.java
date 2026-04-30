package com.ween.service;

import com.ween.exception.ServiceUnavailableException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private MimeMessage mimeMessage;
    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@ween.az");
        ReflectionTestUtils.setField(emailService, "appName", "Ween");
    }

    @Test
    @DisplayName("Send verification email – success")
    void sendVerificationEmail_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() ->
                emailService.sendVerificationEmail("test@e.com", "Test User", "http://verify"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Send verification email – SMTP failure throws RuntimeException")
    void sendVerificationEmail_failure() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() ->
                emailService.sendVerificationEmail("t@e.com", "User", "http://verify"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP error");  // just verify it's the right exception
    }


    @Test
    @DisplayName("Send password reset email – success")
    void sendPasswordResetEmail_success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() ->
                emailService.sendPasswordResetEmail("t@e.com", "User", "http://reset"));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Send password reset email – SMTP failure throws RuntimeException")
    void sendPasswordResetEmail_failure() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() ->
                emailService.sendPasswordResetEmail("t@e.com", "User", "http://reset"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP error");
    }
}
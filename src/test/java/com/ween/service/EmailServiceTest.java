package com.ween.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@ween.az");
        ReflectionTestUtils.setField(emailService, "appName", "Ween");
    }

    @Test
    void sendVerificationEmailPreparesAndSendsMessage() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendVerificationEmail("user@example.com", "Ali Aliyev", "http://verify-link");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmailPreparesAndSendsMessage() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail("user@example.com", "Ali Aliyev", "http://reset-link");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendOrganizerInvitationEmailPreparesAndSendsMessage() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOrganizerInvitationEmail("user@example.com", "My Org", "http://approve", "http://reject");

        verify(mailSender).send(mimeMessage);
    }
}

package com.ween.service;

import com.ween.entity.QrToken;
import com.ween.entity.User;
import com.ween.exception.QrTokenExpiredException;
import com.ween.exception.QrTokenInvalidException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.EventRepository;
import com.ween.repository.QrTokenRepository;
import com.ween.repository.UserRepository;
import com.ween.security.AesUtil;
import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrServiceTest {

    @Mock private QrTokenRepository qrTokenRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AesUtil aesUtil;
    @Mock private SecurityUtil securityUtil;
    @Mock private RegistrationService registrationService;

    private QrService qrService;

    @BeforeEach
    void setUp() {
        qrService = new QrService(
                qrTokenRepository,
                eventRepository,
                userRepository,
                jwtUtil,
                aesUtil,
                securityUtil,
                registrationService
        );
        ReflectionTestUtils.setField(qrService, "tokenValiditySeconds", 30);
    }

    @Test
    void generateQrTokenSavesAndReturnsToken() {
        User user = mock(User.class);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(jwtUtil.generateQrToken(user)).thenReturn("jwt-token");
        when(aesUtil.encrypt("jwt-token")).thenReturn("encrypted-token");

        String result = qrService.generateQrToken("user-1");

        assertThat(result).isEqualTo("encrypted-token");
        verify(qrTokenRepository).revokeAllByUserId("user-1");
        verify(qrTokenRepository).save(any(QrToken.class));
    }

    @Test
    void validateAndDecryptQrTokenReturnsUserIdOnSuccess() {
        when(aesUtil.decrypt("encrypted-token")).thenReturn("jwt-token");
        when(jwtUtil.extractUserId("jwt-token")).thenReturn("user-1");

        User user = mock(User.class);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        QrToken qrToken = mock(QrToken.class);
        when(qrToken.getTokenHash()).thenReturn("encrypted-token");
        when(qrToken.getExpiresAt()).thenReturn(LocalDateTime.now().plusSeconds(10));
        when(qrTokenRepository.findByTokenHashAndIsRevokedFalse("encrypted-token"))
                .thenReturn(Optional.of(qrToken));

        String result = qrService.validateAndDecryptQrToken("encrypted-token");

        assertThat(result).isEqualTo("user-1");
    }

    @Test
    void validateAndDecryptQrTokenThrowsExceptionWhenExpired() {
        when(aesUtil.decrypt("encrypted-token")).thenReturn("jwt-token");
        when(jwtUtil.extractUserId("jwt-token")).thenReturn("user-1");

        User user = mock(User.class);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        QrToken qrToken = mock(QrToken.class);
        when(qrToken.getExpiresAt()).thenReturn(LocalDateTime.now().minusSeconds(10));
        when(qrTokenRepository.findByTokenHashAndIsRevokedFalse("encrypted-token"))
                .thenReturn(Optional.of(qrToken));

        assertThatThrownBy(() -> qrService.validateAndDecryptQrToken("encrypted-token"))
                .isInstanceOf(QrTokenExpiredException.class);

        verify(qrToken).setIsRevoked(true);
        verify(qrTokenRepository).save(qrToken);
    }

    @Test
    void cleanupExpiredTokensDeletesOldRecords() {
        qrService.cleanupExpiredTokens();
        verify(qrTokenRepository).deleteAllByExpiresAtBefore(any(LocalDateTime.class));
    }
}

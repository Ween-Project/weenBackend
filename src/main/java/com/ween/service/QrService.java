package com.ween.service;

import com.ween.dto.response.CheckinResponse;
import com.ween.dto.response.QrResponse;
import com.ween.entity.Event;
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
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class QrService {

    private final QrTokenRepository qrTokenRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AesUtil aesUtil;
    private final SecurityUtil securityUtil;
    private final RegistrationService registrationService;

    @Value("${ween.qr.token-validity-hours:24}")
    private Integer tokenValidityHours;

    @Transactional
    public String generateQrToken(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Revoke existing token if any
        qrTokenRepository.findByUserIdAndIsRevokedFalse(userId).ifPresent(qrToken -> {
            qrToken.setIsRevoked(true);
            qrTokenRepository.save(qrToken);
        });

        // Generate JWT token (using refresh token pattern for QR tokens)
        String jwtToken = jwtUtil.generateRefreshToken(userId);

        // Encrypt token
        String encryptedToken = aesUtil.encrypt(jwtToken);

        // Store token info in database
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(tokenValidityHours);

        QrToken qrToken = QrToken.builder()

                .userId(userId)
                .tokenHash(encryptedToken)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .isRevoked(false)
                .build();

        qrTokenRepository.save(qrToken);
        log.info("QR token generated for user: {}", userId);
        return encryptedToken;
    }

    public String getQrToken(String userId) {
        return qrTokenRepository.findByUserIdAndIsRevokedFalse(userId)
                .map(QrToken::getTokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("QR token not found for user: " + userId));
    }

    @Transactional
    public String validateAndDecryptQrToken(String encryptedToken) {
        try {
            // Decrypt token
            String decryptedToken = aesUtil.decrypt(encryptedToken);

            // Validate JWT token
            String userId = jwtUtil.extractUserId(decryptedToken);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Find and verify token record
            QrToken qrToken = qrTokenRepository.findByUserIdAndIsRevokedFalse(userId)
                    .orElseThrow(() -> new QrTokenInvalidException("Invalid QR token"));

            // Check expiration
            if (LocalDateTime.now().isAfter(qrToken.getExpiresAt())) {
                qrToken.setIsRevoked(true);
                qrTokenRepository.save(qrToken);
                throw new QrTokenExpiredException("QR token has expired");
            }

            // Verify token matches
            if (!encryptedToken.equals(qrToken.getTokenHash())) {
                throw new QrTokenInvalidException("QR token does not match");
            }

            log.info("QR token validated for user: {}", userId);
            return userId;
        } catch (QrTokenExpiredException | QrTokenInvalidException e) {
            throw e;
        } catch (Exception e) {
            log.error("QR token validation failed", e);
            throw new QrTokenInvalidException("Failed to validate QR token", e);
        }
    }

    @Transactional
    public void revokeQrToken(String userId) {
        qrTokenRepository.findByUserIdAndIsRevokedFalse(userId).ifPresent(qrToken -> {
            qrToken.setIsRevoked(true);
            qrTokenRepository.save(qrToken);
        });
        log.info("QR token revoked for user: {}", userId);
    }

    @Transactional
    public void performCheckin(String eventId, String encryptedQrToken) {
        try {
            // Validate and decrypt QR token
            String userId = validateAndDecryptQrToken(encryptedQrToken);

            // Mark user as joined in event registration
            registrationService.markUserAsJoined(eventId, userId);
            log.info("Checkin successful for user: {} at event: {}", userId, eventId);
        } catch (Exception e) {
            log.error("Checkin failed", e);
            throw e;
        }
    }

    public boolean isQrTokenValid(String userId) {
        return qrTokenRepository.findByUserIdAndIsRevokedFalse(userId)
                .filter(qrToken -> LocalDateTime.now().isBefore(qrToken.getExpiresAt()))
                .isPresent();
    }

    public QrToken getQrTokenInfo(String userId) {
        return qrTokenRepository.findByUserIdAndIsRevokedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("QR token not found for user: " + userId));
    }

    @Transactional
    public void refreshQrToken(String userId) {
        // Revoke old token and generate new one
        revokeQrToken(userId);
        generateQrToken(userId);
        log.info("QR token refreshed for user: {}", userId);
    }

    public LocalDateTime getQrTokenExpiryTime(String userId) {
        return getQrTokenInfo(userId).getExpiresAt();
    }

    public Integer getRemainingQrTokenValidityHours(String userId) {
        QrToken qrToken = getQrTokenInfo(userId);
        long hoursRemaining = java.time.temporal.ChronoUnit.HOURS.between(
                LocalDateTime.now(),
                qrToken.getExpiresAt()
        );
        return Math.max(0, (int) hoursRemaining);
    }


    public QrResponse generateQrCode(String userId) {
        return null;
    }

    public CheckinResponse checkinParticipant(@NotBlank(message = "Event ID is required") String eventId, @NotBlank(message = "QR token is required") String qrToken) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        String currentUserId = securityUtil.getCurrentUserId();
        if (!event.getOrganizationId().equals(currentUserId)) {
            throw new AccessDeniedException("Only the event owner can perform check-in");
        }

        String participantUserId = validateAndDecryptQrToken(qrToken);
        User participant = userRepository.findById(participantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + participantUserId));

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

    public Object getLiveEventStats(String id) {
        return null;
    }
}

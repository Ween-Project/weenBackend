package com.ween.service;

import com.ween.dto.response.CheckinResponse;
import com.ween.entity.*;
import com.ween.exception.*;
import com.ween.repository.*;
import com.ween.security.AesUtil;
import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrServiceTest {

    @Mock private QrTokenRepository qrTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private AesUtil aesUtil;
    @Mock private JwtUtil jwtUtil;
    @Mock private SecurityUtil securityUtil;
    @Mock private RegistrationService registrationService;
    @InjectMocks private QrService qrService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qrService, "tokenValidityHours", 24);
        testUser = User.builder().username("u").email("u@e.com").passwordHash("p").fullName("U").build();
        testUser.setId("uid");
    }

    @Test @DisplayName("Generate QR token – success")
    void generateQrToken_success() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.empty());
        when(jwtUtil.generateRefreshToken("uid")).thenReturn("jwt-token");
        when(aesUtil.encrypt("jwt-token")).thenReturn("encrypted-token");
        when(qrTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String token = qrService.generateQrToken("uid");
        assertThat(token).isEqualTo("encrypted-token");
        verify(qrTokenRepository).save(any(QrToken.class));
    }

    @Test @DisplayName("Generate QR token – user not found throws")
    void generateQrToken_userNotFound() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> qrService.generateQrToken("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Generate QR token – revokes existing token")
    void generateQrToken_revokesExisting() {
        QrToken existing = QrToken.builder().userId("uid").isRevoked(false).build();
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.of(existing));
        when(jwtUtil.generateRefreshToken("uid")).thenReturn("jwt-token");
        when(aesUtil.encrypt("jwt-token")).thenReturn("new-token");
        when(qrTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        qrService.generateQrToken("uid");
        assertThat(existing.getIsRevoked()).isTrue();
        verify(qrTokenRepository, times(2)).save(any());
    }

    @Test @DisplayName("Get QR token – found")
    void getQrToken_found() {
        QrToken qt = QrToken.builder().userId("uid").tokenHash("hash").isRevoked(false)
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.of(qt));
        assertThat(qrService.getQrToken("uid")).isEqualTo("hash");
    }

    @Test @DisplayName("Get QR token – not found throws ResourceNotFoundException")
    void getQrToken_notFound() {
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> qrService.getQrToken("uid"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Is QR token valid – true")
    void isQrTokenValid_true() {
        QrToken qt = QrToken.builder().userId("uid").isRevoked(false)
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.of(qt));
        assertThat(qrService.isQrTokenValid("uid")).isTrue();
    }

    @Test @DisplayName("Is QR token valid – expired returns false")
    void isQrTokenValid_expired() {
        QrToken qt = QrToken.builder().userId("uid").isRevoked(false)
                .expiresAt(LocalDateTime.now().minusHours(1)).build();
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.of(qt));
        assertThat(qrService.isQrTokenValid("uid")).isFalse();
    }

    @Test @DisplayName("Is QR token valid – not found returns false")
    void isQrTokenValid_notFound() {
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.empty());
        assertThat(qrService.isQrTokenValid("uid")).isFalse();
    }

    @Test @DisplayName("Revoke QR token – success")
    void revokeQrToken() {
        QrToken qt = QrToken.builder().userId("uid").isRevoked(false).build();
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.of(qt));

        qrService.revokeQrToken("uid");
        assertThat(qt.getIsRevoked()).isTrue();
        verify(qrTokenRepository).save(qt);
    }

    @Test @DisplayName("Checkin participant – success")
    void checkinParticipant_success() {
        Event event = Event.builder().title("Ev").organizationId("org1").build();
        event.setId("eid");

        // validateAndDecryptQrToken internals
        QrToken qt = QrToken.builder().userId("uid").tokenHash("encrypted")
                .isRevoked(false).expiresAt(LocalDateTime.now().plusHours(1)).build();

        when(securityUtil.getCurrentUserId()).thenReturn("org1");
        when(eventRepository.findById("eid")).thenReturn(Optional.of(event));
        when(aesUtil.decrypt("encrypted")).thenReturn("jwt-decrypted");
        when(jwtUtil.extractUserId("jwt-decrypted")).thenReturn("uid");
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(qrTokenRepository.findByUserIdAndIsRevokedFalse("uid")).thenReturn(Optional.of(qt));

        CheckinResponse result = qrService.checkinParticipant("eid", "encrypted");
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CHECKED_IN");
        verify(registrationService).markUserAsJoined("eid", "uid");
    }

    @Test @DisplayName("Checkin – event not found throws")
    void checkin_eventNotFound() {
        when(eventRepository.findById("eid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> qrService.checkinParticipant("eid", "encrypted"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Checkin – access denied for non-owner")
    void checkin_accessDenied() {
        Event event = Event.builder().title("Ev").organizationId("org1").build();
        event.setId("eid");

        when(securityUtil.getCurrentUserId()).thenReturn("other-org");
        when(eventRepository.findById("eid")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> qrService.checkinParticipant("eid", "encrypted"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
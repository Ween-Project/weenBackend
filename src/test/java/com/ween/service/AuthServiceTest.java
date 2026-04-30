package com.ween.service;

import com.ween.dto.request.*;
import com.ween.dto.response.AuthResponse;
import com.ween.entity.*;
import com.ween.enums.UserRole;
import com.ween.exception.*;
import com.ween.repository.*;
import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecurityUtil securityUtil;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private CoinService coinService;
    @Mock private QrService qrService;
    @Mock private NotificationService notificationService;
    @Mock private ReferralRepository referralRepository;

    @InjectMocks private AuthService authService;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser").email("test@example.com")
                .passwordHash("encoded-password").fullName("Test User")
                .role(UserRole.VOLUNTEER).weenCoinBalance(0).referralCode("ABCD1234")
                .build();
        testUser.setId("user-id-1");

        testOrg = Organization.builder()
                .username("testorg").email("org@example.com")
                .passwordHash("encoded-password").organizationName("Test Org")
                .description("A test organization").role(UserRole.ORGANIZATION_ADMIN)
                .build();
        testOrg.setId("org-id-1");
    }

    // ── Registration ───────────────────────────────────────────────────
    @Test @DisplayName("Register user – happy path")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser"); req.setEmail("new@example.com");
        req.setPassword("password123"); req.setFullName("New User");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(organizationRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(i -> { User u=i.getArgument(0); u.setId("nid"); return u; });
        when(jwtUtil.generateAccessToken(any(),any(),any())).thenReturn("at");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("rt");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900000L);

        AuthResponse res = authService.register(req);
        assertThat(res.getAccessToken()).isEqualTo("at");
        verify(coinService).awardSignupBonus(anyString());
    }

    @Test @DisplayName("Register – duplicate email throws")
    void register_duplicateEmail() {
        RegisterRequest req = new RegisterRequest(); req.setEmail("test@example.com"); req.setUsername("x");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(req)).isInstanceOf(AlreadyExistsException.class);
    }

    @Test @DisplayName("Register – duplicate username throws")
    void register_duplicateUsername() {
        RegisterRequest req = new RegisterRequest(); req.setEmail("new@e.com"); req.setUsername("testuser");
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(organizationRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(req)).isInstanceOf(AlreadyExistsException.class);
    }

    @Test @DisplayName("Register – referral code awards coins")
    void register_withReferral() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("u"); req.setEmail("e@e.com"); req.setPassword("p"); req.setFullName("F");
        req.setReferralCode("ABCD1234");
        User referrer = User.builder().build(); referrer.setId("ref-id");

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(organizationRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("e");
        when(userRepository.save(any(User.class))).thenAnswer(i -> { User u=i.getArgument(0); u.setId("nid"); return u; });
        when(userRepository.findByReferralCode("ABCD1234")).thenReturn(Optional.of(referrer));
        when(jwtUtil.generateAccessToken(any(),any(),any())).thenReturn("a");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("r");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(1L);

        authService.register(req);
        verify(coinService).awardReferralBonus(eq("ref-id"), anyString());
    }

    // ── Organization Registration ──────────────────────────────────────
    @Test @DisplayName("Register organization – happy path")
    void registerOrganization_success() {
        RegisterOrganizationRequest req = new RegisterOrganizationRequest();
        req.setUsername("no"); req.setEmail("no@e.com"); req.setPassword("p");
        req.setOrganizationName("Org"); req.setDescription("d");

        when(organizationRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(organizationRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("e");
        when(organizationRepository.save(any())).thenAnswer(i -> { Organization o=i.getArgument(0); o.setId("oid"); return o; });
        when(jwtUtil.generateAccessToken(any(),any(),any())).thenReturn("a");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("r");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(1L);

        AuthResponse res = authService.registerOrganization(req);
        assertThat(res.getOrganization().getOrganizationName()).isEqualTo("Org");
    }

    @Test @DisplayName("Register organization – duplicate email throws")
    void registerOrg_dupEmail() {
        RegisterOrganizationRequest req = new RegisterOrganizationRequest();
        req.setEmail("org@example.com"); req.setUsername("x");
        when(organizationRepository.existsByEmail("org@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.registerOrganization(req)).isInstanceOf(AlreadyExistsException.class);
    }

    // ── Login ──────────────────────────────────────────────────────────
    @Test @DisplayName("User login – success")
    void login_success() {
        LoginRequest req = new LoginRequest("test@example.com", "password");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(),any(),any())).thenReturn("a");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("r");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(1L);
        when(qrService.isQrTokenValid(any())).thenReturn(true);
        when(qrService.getQrToken(any())).thenReturn("qr");

        AuthResponse res = authService.login(req);
        assertThat(res.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test @DisplayName("Login – wrong password throws")
    void login_wrongPassword() {
        LoginRequest req = new LoginRequest("test@example.com", "wrong");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("Login – user not found throws")
    void login_notFound() {
        LoginRequest req = new LoginRequest("x@x.com", "p");
        when(userRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(UnauthorizedException.class);
    }

    @Test @DisplayName("Organization login – success")
    void loginOrg_success() {
        LoginRequest req = new LoginRequest("org@example.com", "password");
        when(organizationRepository.findByEmail("org@example.com")).thenReturn(Optional.of(testOrg));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(),any(),any())).thenReturn("a");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("r");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(1L);

        AuthResponse res = authService.loginOrganization(req);
        assertThat(res.getOrganization().getEmail()).isEqualTo("org@example.com");
    }

    // ── Token Refresh ──────────────────────────────────────────────────
    @Test @DisplayName("Refresh token – user")
    void refreshToken_user() {
        when(jwtUtil.extractUserId("rt")).thenReturn("user-id-1");
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(any(),any(),any())).thenReturn("new");
        assertThat(authService.refreshToken("rt")).isEqualTo("new");
    }

    @Test @DisplayName("Refresh token – organization")
    void refreshToken_org() {
        when(jwtUtil.extractUserId("rt")).thenReturn("org-id-1");
        when(userRepository.findById("org-id-1")).thenReturn(Optional.empty());
        when(organizationRepository.findById("org-id-1")).thenReturn(Optional.of(testOrg));
        when(jwtUtil.generateAccessToken(any(),any(),any())).thenReturn("new");
        assertThat(authService.refreshToken("rt")).isEqualTo("new");
    }

    @Test @DisplayName("Refresh token – not found throws")
    void refreshToken_notFound() {
        when(jwtUtil.extractUserId("rt")).thenReturn("x");
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        when(organizationRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.refreshToken("rt")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Email Verification ─────────────────────────────────────────────
    @Test @DisplayName("Verify email – success")
    void verifyEmail_success() {
        EmailVerificationToken t = EmailVerificationToken.builder()
                .userId("user-id-1").token("tok").expiresAt(LocalDateTime.now().plusHours(1)).isUsed(false).build();
        when(emailVerificationTokenRepository.findByTokenAndIsUsedFalse("tok")).thenReturn(Optional.of(t));
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        authService.verifyEmail("tok");
        verify(userRepository).save(argThat(u -> u.getIsEmailVerified()));
    }

    @Test @DisplayName("Verify email – expired token throws")
    void verifyEmail_expired() {
        EmailVerificationToken t = EmailVerificationToken.builder()
                .userId("user-id-1").token("tok").expiresAt(LocalDateTime.now().minusHours(1)).isUsed(false).build();
        when(emailVerificationTokenRepository.findByTokenAndIsUsedFalse("tok")).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> authService.verifyEmail("tok")).isInstanceOf(InvalidTokenException.class);
    }

    @Test @DisplayName("Verify email – invalid token throws")
    void verifyEmail_invalid() {
        when(emailVerificationTokenRepository.findByTokenAndIsUsedFalse("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.verifyEmail("bad")).isInstanceOf(InvalidTokenException.class);
    }

    // ── Password Reset ─────────────────────────────────────────────────
    @Test @DisplayName("Reset password with token – success")
    void resetPassword_success() {
        PasswordResetToken prt = PasswordResetToken.builder()
                .userId("user-id-1").token("tok").expiresAt(LocalDateTime.now().plusHours(1)).isUsed(false).build();
        ResetPasswordRequest req = new ResetPasswordRequest(); req.setToken("tok"); req.setNewPassword("new");

        when(passwordResetTokenRepository.findByTokenAndIsUsedFalse("tok")).thenReturn(Optional.of(prt));
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("new")).thenReturn("enc-new");

        authService.resetPasswordWithToken(req);
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("enc-new")));
    }

    @Test @DisplayName("Reset password – expired token throws")
    void resetPassword_expired() {
        PasswordResetToken prt = PasswordResetToken.builder()
                .userId("x").token("tok").expiresAt(LocalDateTime.now().minusHours(1)).isUsed(false).build();
        ResetPasswordRequest req = new ResetPasswordRequest(); req.setToken("tok"); req.setNewPassword("n");
        when(passwordResetTokenRepository.findByTokenAndIsUsedFalse("tok")).thenReturn(Optional.of(prt));
        assertThatThrownBy(() -> authService.resetPasswordWithToken(req)).isInstanceOf(InvalidTokenException.class);
    }

    // ── Change Password ────────────────────────────────────────────────
    @Test @DisplayName("Change password – success")
    void changePassword_success() {
        ChangePasswordRequest req = new ChangePasswordRequest(); req.setOldPassword("old"); req.setNewPassword("new");
        when(securityUtil.getCurrentUserId()).thenReturn("user-id-1");
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("old", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("enc");
        authService.changePasswordForCurrentUser(req);
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("enc")));
    }

    @Test @DisplayName("Change password – wrong old password throws")
    void changePassword_wrongOld() {
        ChangePasswordRequest req = new ChangePasswordRequest(); req.setOldPassword("wrong"); req.setNewPassword("n");
        when(securityUtil.getCurrentUserId()).thenReturn("user-id-1");
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);
        assertThatThrownBy(() -> authService.changePasswordForCurrentUser(req)).isInstanceOf(UnauthorizedException.class);
    }

    // ── Send Verification ──────────────────────────────────────────────
    @Test @DisplayName("Send verification – already verified throws")
    void sendVerification_alreadyVerified() {
        testUser.setIsEmailVerified(true);
        when(securityUtil.getCurrentUserId()).thenReturn("user-id-1");
        when(userRepository.findById("user-id-1")).thenReturn(Optional.of(testUser));
        assertThatThrownBy(() -> authService.sendVerificationTokenForCurrentUser()).isInstanceOf(AlreadyExistsException.class);
    }
}

package com.ween.service;

import com.ween.dto.request.ChangePasswordRequest;
import com.ween.dto.request.LoginRequest;
import com.ween.dto.request.RegisterRequest;
import com.ween.dto.response.AuthResponse;
import com.ween.entity.User;
import com.ween.enums.UserRole;
import com.ween.exception.AlreadyExistsException;
import com.ween.exception.UnauthorizedException;
import com.ween.repository.EmailVerificationTokenRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.PasswordResetTokenRepository;
import com.ween.repository.UserRepository;
import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Mock private ReferralService referralService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                organizationRepository,
                passwordEncoder,
                securityUtil,
                jwtUtil,
                emailService,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                coinService,
                qrService,
                notificationService,
                referralService);
        ReflectionTestUtils.setField(authService, "verifyEmailBaseUrl", "http://localhost:5001/verify");
        ReflectionTestUtils.setField(authService, "resetPasswordBaseUrl", "http://localhost:5001/reset-password");
    }

    @Test
    void registerCreatesUserAwardsCoinsAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest(
                "ali",
                "ali@example.com",
                "password123",
                "Ali Valiyev",
                LocalDate.of(2001, 1, 1),
                null,
                "BDU",
                "CS",
                "3",
                "java, spring",
                "teamwork",
                "REF123");
        User savedUser = User.builder()
                .username("ali")
                .email("ali@example.com")
                .passwordHash("encoded")
                .fullName("Ali Valiyev")
                .role(UserRole.VOLUNTEER)
                .isEmailVerified(false)
                .weenCoinBalance(0)
                .build();
        savedUser.setId("user-1");

        when(userRepository.existsByEmail("ali@example.com")).thenReturn(false);
        when(organizationRepository.existsByEmail("ali@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("ali")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateAccessToken("user-1", "ali@example.com", UserRole.VOLUNTEER)).thenReturn("access");
        when(jwtUtil.generateRefreshToken("user-1")).thenReturn("refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900_000L);

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(response.getUser().getUsername()).isEqualTo("ali");
        verify(coinService).awardSignupBonus("user-1");
        verify(referralService).processReferralAtSignup("REF123", "user-1");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.VOLUNTEER);
    }

    @Test
    void registerFailsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("ali@example.com");
        request.setUsername("ali");
        when(userRepository.existsByEmail("ali@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokensForValidVolunteerCredentials() {
        User user = User.builder()
                .email("ali@example.com")
                .username("ali")
                .passwordHash("encoded")
                .role(UserRole.VOLUNTEER)
                .isEmailVerified(true)
                .weenCoinBalance(75)
                .build();
        user.setId("user-1");
        when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtUtil.generateAccessToken("user-1", "ali@example.com", UserRole.VOLUNTEER)).thenReturn("access");
        when(jwtUtil.generateRefreshToken("user-1")).thenReturn("refresh");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(900_000L);

        AuthResponse response = authService.login(new LoginRequest("ali@example.com", "password123"));

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getUser().getWeenCoinBalance()).isEqualTo(75);
    }

    @Test
    void loginFailsWhenPasswordDoesNotMatch() {
        User user = User.builder()
                .email("ali@example.com")
                .passwordHash("encoded")
                .role(UserRole.VOLUNTEER)
                .build();
        user.setId("user-1");
        when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ali@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void changePasswordUpdatesCurrentUserPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-password");
        request.setNewPassword("new-password");
        User user = User.builder()
                .email("ali@example.com")
                .passwordHash("old-encoded")
                .role(UserRole.VOLUNTEER)
                .build();
        user.setId("user-1");
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "old-encoded")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded");

        authService.changePasswordForCurrentUser(request);

        assertThat(user.getPasswordHash()).isEqualTo("new-encoded");
        verify(userRepository).save(user);
    }
}

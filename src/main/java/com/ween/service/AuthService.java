package com.ween.service;

import com.ween.dto.request.LoginRequest;
import com.ween.dto.request.ChangePasswordRequest;
import com.ween.dto.request.RegisterRequest;
import com.ween.dto.request.RegisterOrganizationRequest;
import com.ween.dto.request.ResetPasswordRequest;
import com.ween.dto.response.AuthResponse;
import com.ween.dto.response.OrganizationResponse;
import com.ween.dto.response.UserResponse;
import com.ween.entity.EmailVerificationToken;
import com.ween.entity.PasswordResetToken;
import com.ween.entity.User;
import com.ween.entity.Organization;
import com.ween.enums.UserRole;
import com.ween.exception.AlreadyExistsException;
import com.ween.exception.InvalidTokenException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.exception.UnauthorizedException;
import com.ween.enums.NotificationType;
import com.ween.repository.EmailVerificationTokenRepository;
import com.ween.repository.PasswordResetTokenRepository;
import com.ween.repository.UserRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final CoinService coinService;
    private final QrService qrService;
    private final NotificationService notificationService;
    private final ReferralService referralService;
    private final CloudinaryService cloudinaryService;
    @Value("${ween.frontend.verify-url:http://localhost:5001/verify}")
    private String verifyEmailBaseUrl;

    @Value("${ween.frontend.reset-password-url:http://localhost:5001/reset-password}")
    private String resetPasswordBaseUrl;

    @Value("${admin.seed.email:super_admin@ween.com}")
    private String adminEmail;

    @Transactional
    public AuthResponse register(RegisterRequest request, MultipartFile profilePhoto, MultipartFile banner, String referralCode) {
        if (request.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new IllegalArgumentException("Registration not allowed for this email address");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail()) || organizationRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Email already registered: " + request.getEmail());
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AlreadyExistsException("Username already taken: " + request.getUsername());
        }

        String profilePhotoUrl = null;
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            profilePhotoUrl = uploadImage(profilePhoto, "users/profiles");
        }

        String bannerUrl = null;
        if (banner != null && !banner.isEmpty()) {
            bannerUrl = uploadImage(banner, "users/banners");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .birthDate(request.getBirthDate())
                .phone(request.getPhone())
                .university(request.getUniversity())
                .major(request.getMajor())
                .course(request.getCourse())
                .interests(convertToJsonArray(request.getInterests()))
                .skills(convertToJsonArray(request.getSkills()))
                .profilePhotoUrl(profilePhotoUrl)
                .bannerUrl(bannerUrl)
                .role(UserRole.VOLUNTEER)
                .referralCode(generateReferralCode())
                .weenCoinBalance(0)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        try {
            notificationService.createNotification(
                    savedUser.getId(),
                    NotificationType.SYSTEM,
                    "Welcome to Ween",
                    "Your account has been created successfully. Your QR token is ready for event check-ins."
            );
        } catch (Exception e) {
            log.warn("Failed to create welcome notification for user: {}", savedUser.getId(), e);
        }

        try {
            createAndSendEmailVerification(savedUser);
        } catch (Exception e) {
            // Registration should still succeed even if email provider is temporarily unavailable.
            log.error("Failed to create/send verification email for user: {}", savedUser.getEmail(), e);
        }

        // Award signup bonus
        coinService.awardSignupBonus(savedUser.getId());

        // Handle referral if provided
        // Handle referral if provided
        if (referralCode != null && !referralCode.trim().isEmpty()) {
            try {
                referralService.processReferralAtSignup(referralCode.trim(), savedUser.getId());
            } catch (Exception e) {
                log.warn("Failed to process referral code during registration", e);
            }
        }

        // Generate tokens for immediate login
        String accessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail(),savedUser.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        UserResponse userResponse = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .isEmailVerified(savedUser.getIsEmailVerified())
                .weenCoinBalance(savedUser.getWeenCoinBalance())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }


    @Transactional
    public AuthResponse registerOrganization(RegisterOrganizationRequest request, MultipartFile logo, MultipartFile banner) {
        String orgEmail = request.getEmail();
        String orgUsername = request.getUsername();

        // Check if email already exists in organizations
        if (organizationRepository.existsByEmail(orgEmail)|| userRepository.existsByEmail(orgEmail)) {
            throw new AlreadyExistsException("Email already registered: " + orgEmail);
        }

        // Check if username already exists in organizations
        if (organizationRepository.existsByUsername(orgUsername)) {
            throw new AlreadyExistsException("Username already taken: " + orgUsername);
        }

        String logoUrl = null;
        if (logo != null && !logo.isEmpty()) {
            logoUrl = uploadImage(logo, "organizations/logos");
        }

        String bannerUrl = null;
        if (banner != null && !banner.isEmpty()) {
            bannerUrl = uploadImage(banner, "organizations/banners");
        }

        // Create organization directly (without creating user account)
        Organization organization = Organization.builder()
                .username(orgUsername)
                .email(orgEmail)
                .logoUrl(logoUrl)
                .bannerUrl(bannerUrl)
                .website(request.getWebsite())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .organizationName(request.getOrganizationName())
                .description(request.getDescription())
                .role(UserRole.ORGANIZATION_ADMIN)
                .build();

        Organization savedOrganization = organizationRepository.save(organization);
        log.info("Organization registered successfully: {}", savedOrganization.getEmail());

        try {
            notificationService.createNotification(
                    savedOrganization.getId(),
                    NotificationType.SYSTEM,
                    "Organization account created",
                    "Your organization account is ready and can now create and manage events."
            );
        } catch (Exception e) {
            log.warn("Failed to create welcome notification for organization: {}", savedOrganization.getId(), e);
        }

        // Generate tokens for immediate login
        String accessToken = jwtUtil.generateAccessToken(savedOrganization.getId(), savedOrganization.getEmail(), UserRole.ORGANIZATION_ADMIN);
        String refreshToken = jwtUtil.generateRefreshToken(savedOrganization.getId());

        // Create response using UserResponse (can be reused)
        OrganizationResponse orgResponse = OrganizationResponse.builder()
                .id(savedOrganization.getId())
                .username(savedOrganization.getUsername())
                .email(savedOrganization.getEmail())
                .website(savedOrganization.getWebsite())
                .logoUrl(savedOrganization.getLogoUrl())
                .description(savedOrganization.getDescription())
                .organizationName(savedOrganization.getOrganizationName())
                .role(savedOrganization.getRole())
                .createdAt(LocalDateTime.now())
                .isVerified(savedOrganization.getIsVerified())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .organization(orgResponse)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (Boolean.TRUE.equals(user.getBanned())) {
            throw new UnauthorizedException("This account has been suspended");
        }
        if (user.getRole() == UserRole.ORGANIZATION_ADMIN) {
            throw new UnauthorizedException("Please sign in using the organization account type");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        log.info("User logged in successfully: {}", user.getEmail());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isEmailVerified(user.getIsEmailVerified())
                .weenCoinBalance(user.getWeenCoinBalance())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    public AuthResponse loginOrganization(LoginRequest request) {
        Organization organization = organizationRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), organization.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(organization.getId(), organization.getEmail(), organization.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(organization.getId());

        log.info("Organization logged in successfully: {}", organization.getEmail());

        OrganizationResponse orgResponse = OrganizationResponse.builder()
                .id(organization.getId())
                .username(organization.getUsername())
                .email(organization.getEmail())
                .organizationName(organization.getOrganizationName())
                .description(organization.getDescription())
                .role(organization.getRole())
                .isVerified(organization.getIsVerified())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .organization(orgResponse)
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .build();
    }

    public String refreshToken(String refreshTokenStr) {
        try {
            String accountId = jwtUtil.extractUserId(refreshTokenStr);

            User user = userRepository.findById(accountId).orElse(null);

            if (user != null) {
                if (Boolean.TRUE.equals(user.getBanned())) {
                    throw new UnauthorizedException("This account has been suspended");
                }
                log.info("Refresh token used by User: {}", user.getEmail());
                return jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
            }

            Organization organization = organizationRepository.findById(accountId).orElse(null);

            if (organization != null) {
                log.info("Refresh token used by Organization: {}", organization.getEmail());
                return jwtUtil.generateAccessToken(organization.getId(), organization.getEmail(), UserRole.ORGANIZATION_ADMIN);
            }

            throw new ResourceNotFoundException("Account not found for the provided token");

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new UnauthorizedException("Invalid refresh token");
        }
    }


    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        Organization organization = null;

        if (user == null) {
            organization = organizationRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with email: " + email));
        }

        String accountId = user != null ? user.getId() : organization.getId();
        String displayName = user != null ? user.getFullName() : organization.getOrganizationName();
        String accountEmail = user != null ? user.getEmail() : organization.getEmail();

        passwordResetTokenRepository.deleteByUserId(accountId);

        String rawToken = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(accountId)
                .token(rawToken)
                .expiresAt(expiresAt)
                .isUsed(false)
                .build();
        passwordResetTokenRepository.save(token);

        String resetLink = resetPasswordBaseUrl + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        emailService.sendPasswordResetEmail(accountEmail, displayName, resetLink);
        log.info("Password reset email sent to: {}", email);
    }

    @Transactional
    public void resetPasswordWithToken(@Valid ResetPasswordRequest request) {
        String token = request.getToken();
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Reset token is required");
        }

        PasswordResetToken passwordResetToken = passwordResetTokenRepository
                .findByTokenAndIsUsedFalse(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        if (passwordResetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset token has expired");
        }

        User user = userRepository.findById(passwordResetToken.getUserId()).orElse(null);

        if (user != null) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            log.info("Password reset successfully via token for user: {}", user.getEmail());
        } else {
            Organization organization = organizationRepository.findById(passwordResetToken.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            organization.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            organizationRepository.save(organization);
            log.info("Password reset successfully via token for organization: {}", organization.getEmail());
        }

        // Invalidate token immediately after successful password reset.
        passwordResetToken.setIsUsed(true);
        passwordResetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(passwordResetToken);
    }

    @Transactional
    public void changePasswordForCurrentUser(@Valid ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword();
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new UnauthorizedException("Old password is required");
        }

        String accountId = securityUtil.getCurrentUserId();

        User user = userRepository.findById(accountId).orElse(null);
        if (user != null) {
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                throw new UnauthorizedException("Old password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            log.info("Password changed successfully for User: {}", user.getEmail());
            return;
        }

        Organization organization = organizationRepository.findById(accountId).orElse(null);
        if (organization != null) {

            if (!passwordEncoder.matches(oldPassword, organization.getPasswordHash())) {
                throw new UnauthorizedException("Old password is incorrect");
            }
            organization.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            organizationRepository.save(organization);
            log.info("Password changed successfully for Organization: {}", organization.getEmail());
            return;
        }

        throw new ResourceNotFoundException("Account not found");
    }

    private String generateReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String convertToJsonArray(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            // Split by comma and create JSON array
            List<String> items = Arrays.asList(value.split(","));
            List<String> trimmedItems = new ArrayList<>();
            for (String item : items) {
                trimmedItems.add(item.trim());
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(trimmedItems);
        } catch (Exception e) {
            log.warn("Failed to convert string to JSON array: {}", value, e);
            return null;
        }
    }

    private String uploadImage(MultipartFile file, String folder) {
        try {
            return cloudinaryService.uploadFile(file, folder);
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Image upload failed", e);
        }
    }

    @Transactional
    public void logout() {
        try {
            String userId = securityUtil.getCurrentUserId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            log.info("User logged out successfully: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Logout process error for user", e);
            throw new UnauthorizedException("Logout failed");
        }
    }

    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenAndIsUsedFalse(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification token has expired");
        }

        User user = userRepository.findById(verificationToken.getUserId()).orElse(null);

        if (user != null) {
            user.setIsEmailVerified(true);
            userRepository.save(user);
            log.info("Email verified successfully for user: {}", user.getEmail());
        } else {
            Organization organization = organizationRepository.findById(verificationToken.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

            organization.setVerified(true);
            organizationRepository.save(organization);
            log.info("Email verified successfully for organization: {}", organization.getEmail());
        }

        verificationToken.setIsUsed(true);
        verificationToken.setVerifiedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(verificationToken);
    }

    public void sendVerificationTokenForCurrentUser() {
        String userId = securityUtil.getCurrentUserId();
        User user = userRepository.findById(userId).orElse(null);

        if (user != null) {
            if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
                throw new AlreadyExistsException("Email is already verified");
            }

            createAndSendEmailVerification(user);
            return;
        }

        Organization organization = organizationRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (Boolean.TRUE.equals(organization.getIsVerified())) {
            throw new AlreadyExistsException("Email is already verified");
        }

        createAndSendEmailVerification(organization);
    }

    public void sendPasswordResetLink(@NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email) {
        initiatePasswordReset(email);
    }

    private void createAndSendEmailVerification(User user) {
        emailVerificationTokenRepository.deleteByUserId(user.getId());

        String rawToken = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(rawToken)
                .expiresAt(expiresAt)
                .isUsed(false)
                .build();

        emailVerificationTokenRepository.save(token);

        String verificationLink = verifyEmailBaseUrl + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationLink);
    }

    private void createAndSendEmailVerification(Organization organization) {
        emailVerificationTokenRepository.deleteByUserId(organization.getId());

        String rawToken = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(organization.getId())
                .token(rawToken)
                .expiresAt(expiresAt)
                .isUsed(false)
                .build();

        emailVerificationTokenRepository.save(token);

        String verificationLink = verifyEmailBaseUrl + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        emailService.sendVerificationEmail(organization.getEmail(), organization.getOrganizationName(), verificationLink);
    }
}

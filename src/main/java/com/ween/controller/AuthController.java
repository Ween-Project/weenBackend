package com.ween.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

import com.ween.dto.request.ChangePasswordRequest;
import com.ween.dto.request.ForgotPasswordRequest;
import com.ween.dto.request.LoginRequest;
import com.ween.dto.request.RefreshTokenRequest;
import com.ween.dto.request.RegisterOrganizationRequest;
import com.ween.dto.request.RegisterRequest;
import com.ween.dto.request.ResetPasswordRequest;
import com.ween.dto.request.VerifyEmailRequest;
import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.AuthResponse;
import com.ween.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    @Value("${ween.jwt.refresh-token-expiry:604800}")
    private long refreshTokenExpirySeconds;

    @Value("${ween.jwt.access-token-expiry:900}")
    private long accessTokenExpirySeconds;

    @Value("${ween.cookie.secure:false}")
    private boolean secureCookies;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account with optional referral code")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User already exists", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @Parameter(description = "Optional referral code")
            @RequestParam(value = "ref", required = false) String referralCode) {
        try {
            if (referralCode != null && !referralCode.isEmpty()) {
                request.setReferralCode(referralCode);
            }
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .headers(authCookies(response))
                    .body(ApiResponse.ok(response, "User registered successfully"));
        } catch (Exception e) {
            log.error("Registration failed", e);
            throw e;
        }
    }

    @PostMapping(value = "/register/organization", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register new organization", description = "Create a new organization account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Organization registered successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Organization already exists", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthResponse>> registerOrganization(
            @io.swagger.v3.oas.annotations.Parameter(content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json")) @Valid @org.springframework.web.bind.annotation.RequestPart("request") RegisterOrganizationRequest request,
            @RequestParam(value = "logo", required = false) MultipartFile logo) {
        try {
            AuthResponse response = authService.registerOrganization(request, logo);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .headers(authCookies(response))
                    .body(ApiResponse.ok(response, "Organization registered successfully"));
        } catch (Exception e) {
            log.error("Organization registration failed", e);
            throw e;
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and receive access token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok()
                    .headers(authCookies(response))
                    .body(ApiResponse.ok(response, "Login successful"));
        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            throw e;
        }
    }

    @PostMapping("/login/organization")
    @Operation(summary = "Organization login", description = "Authenticate organization and receive access token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthResponse>> loginOrganization(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.loginOrganization(request);
            return ResponseEntity.ok()
                    .headers(authCookies(response))
                    .body(ApiResponse.ok(response, "Organization login successful"));
        } catch (Exception e) {
            log.error("Organization login failed for email: {}", request.getEmail(), e);
            throw e;
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Use refresh token to get a new access token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = String.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid refresh token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<String>> refresh(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshCookie,
            @Valid @RequestBody(required = false) RefreshTokenRequest request) {
        try {
            String refreshToken = refreshCookie;
            if ((refreshToken == null || refreshToken.isBlank()) && request != null) {
                refreshToken = request.getRefreshToken();
            }

            String response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessTokenCookie(response).toString())
                    .body(ApiResponse.ok(response, "Token refreshed successfully"));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw e;
        }
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "Bearer")
    @Operation(summary = "User logout", description = "Invalidate the current access token. Requires authentication.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token required or invalid", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> logout() {
        try {
            authService.logout();
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookie(ACCESS_TOKEN_COOKIE).toString())
                    .header(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_TOKEN_COOKIE).toString())
                    .body(ApiResponse.ok(null, "Logout successful"));
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw e;
        }

    }

    @GetMapping("/verify-token")
    @SecurityRequirement(name = "Bearer")
    @Operation(summary = "Resend verification email", description = "Generate a new email verification token for current user and send it by email. Requires authentication.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification email sent successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email is already verified", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token required or invalid", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> generateVerifyToken() {
        try {
            authService.sendVerificationTokenForCurrentUser();
            return ResponseEntity.ok(ApiResponse.ok(null, "Verification email sent successfully"));
        } catch (Exception e) {
            log.error("Generating verification token failed", e);
            throw e;
        }
    }

    @PostMapping("/verify-token")
    @Operation(summary = "Verify email with token", description = "Complete email verification by submitting the verification token from email link")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired verification token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> verifyToken(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            authService.verifyEmail(request.getToken());
            return ResponseEntity.ok(ApiResponse.ok(null, "Email verified successfully"));
        } catch (Exception e) {
            log.error("Email token verification failed", e);
            throw e;
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Request password reset link to be sent to email")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset link sent successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.sendPasswordResetLink(request.getEmail());
            return ResponseEntity.ok(ApiResponse.ok(null, "Password reset link sent to your email"));
        } catch (Exception e) {
            log.error("Forgot password request failed for email: {}", request.getEmail(), e);
            throw e;
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token", description = "Reset account password using reset token from email")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPasswordWithToken(request);
            return ResponseEntity.ok(ApiResponse.ok(null, "Password reset successfully"));
        } catch (Exception e) {
            log.error("Password reset failed", e);
            throw e;
        }
    }

    @PostMapping("/change-password")
    @SecurityRequirement(name = "Bearer")
    @Operation(summary = "Change current password", description = "Change current user password using old and new password. Requires authentication.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password changed successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid current password", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Bearer token required or invalid", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            authService.changePasswordForCurrentUser(request);
            return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
        } catch (Exception e) {
            log.error("Password change failed", e);
            throw e;
        }
    }

    private HttpHeaders authCookies(AuthResponse response) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessTokenCookie(response.getAccessToken()).toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie(response.getRefreshToken()).toString());
        return headers;
    }

    private ResponseCookie accessTokenCookie(String token) {
        return tokenCookie(ACCESS_TOKEN_COOKIE, token, accessTokenExpirySeconds);
    }

    private ResponseCookie refreshTokenCookie(String token) {
        return tokenCookie(REFRESH_TOKEN_COOKIE, token, refreshTokenExpirySeconds);
    }

    private ResponseCookie tokenCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie clearCookie(String name) {
        return tokenCookie(name, "", 0);
    }
}

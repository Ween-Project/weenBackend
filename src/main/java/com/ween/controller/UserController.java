package com.ween.controller;

import com.ween.dto.request.UpdateProfilePhotoRequest;
import com.ween.dto.request.UpdateProfileRequest;
import com.ween.dto.response.*;
import com.ween.entity.Certificate;
import com.ween.entity.User;
import com.ween.exception.UnauthorizedException;
import com.ween.mapper.CertificateMapper;
import com.ween.mapper.UserMapper;
import com.ween.security.SecurityUtil;
import com.ween.service.CertificateService;
import com.ween.service.RegistrationService;
import com.ween.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and event management endpoints")
public class UserController {

    private final UserService userService;
    private final RegistrationService registrationService;
    private final CertificateService certificateService;
    private final SecurityUtil securityUtil;
    private final UserMapper userMapper;
    private final CertificateMapper certificateMapper;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieve authenticated user's profile information")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<User>> getCurrentUserProfile() {
        try {
            String userId = getCurrentUserId();
            User response = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponse.ok(response, "Profile retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve user profile", e);
            throw e;
        }
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Update authenticated user's profile information")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            String userId = getCurrentUserId();
            User response = userService.updateProfile(userId, request);
            return ResponseEntity.ok(ApiResponse.ok(response, "Profile updated successfully"));
        } catch (Exception e) {
            log.error("Failed to update user profile", e);
            throw e;
        }
    }


    @PutMapping("/profile-photo")
    @Transactional
    @Operation(summary = "Update current user profile photo", description = "Update profile photo (current user only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile photo updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<User>> updateUserPhoto(
            @Valid @RequestBody UpdateProfilePhotoRequest request) {
        String userId = null;
        try {
            userId = securityUtil.getCurrentUserId();

            User response = userService.updateUserPhoto(userId, request);

            return ResponseEntity.ok(ApiResponse.ok(response, "Profile photo updated successfully"));
        } catch (Exception e) {

            log.error("Failed to update user profile photo for user: {}", userId, e);
            throw e;
        }
    }


    @GetMapping("/@{username}")
    @Operation(summary = "Get public user profile", description = "Retrieve public profile information for a user by username")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<PublicProfileResponse>> getPublicProfile(
            @Parameter(description = "Username", required = true)
            @PathVariable String username) {
        try {
            PublicProfileResponse response = userService.getPublicProfile(username);
            return ResponseEntity.ok(ApiResponse.ok(response, "Profile retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve public profile for username: {}", username, e);
            throw e;
        }
    }

    @GetMapping("/me/events")
    @Operation(summary = "Get user's attended events", description = "Retrieve paginated list of events user participated in")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getUserEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            String userId = getCurrentUserId();
            Page<EventResponse> response = registrationService.getUserEvents(userId, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Events retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve user events for user: {}", getCurrentUserId(), e);
            throw e;
        }
    }

    @GetMapping("/me/certificates")
    @Operation(summary = "Get user's certificates", description = "Retrieve paginated list of user's earned certificates")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Certificates retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<Certificate>>> getUserCertificates(
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            String userId = getCurrentUserId();
            List<Certificate> response = certificateService.getUserCertificates(userId);
            return ResponseEntity.ok(ApiResponse.ok(response, "Certificates retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve certificates for user: {}", getCurrentUserId(), e);
            throw e;
        }
    }

    @GetMapping("/me/coins")
    @Operation(summary = "Get user's coin information", description = "Retrieve user's coin balance and transaction history")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Coin information retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Integer>> getUserCoins() {
        try {
            String userId = getCurrentUserId();
            Integer response = userService.getUserCoinBalance(userId);
            return ResponseEntity.ok(ApiResponse.ok(response, "Coin information retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve coin information for user: {}", getCurrentUserId(), e);
            throw e;
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("User not authenticated");
        }
        return (String) authentication.getPrincipal();
    }
}
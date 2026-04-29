package com.ween.controller;

import com.ween.dto.response.AdminStatsResponse;
import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.OrganizationResponse;
import com.ween.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Admin-only platform management endpoints")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Retrieve pageable list of all platform users (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponse<Page<com.ween.dto.response.UserResponse>>> getAllUsers(
            @Parameter(description = "Filter by username or email") @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            Page<com.ween.dto.response.UserResponse> response = adminService.getAllUsers(search, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Users retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve users", e);
            throw e;
        }
    }

    @PutMapping("/users/{id}/ban")
    @Transactional
    @Operation(summary = "Ban/unban user", description = "Ban or unban a user account (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User ban status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<com.ween.dto.response.UserResponse>> banUnbanUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id,
            @Parameter(description = "Ban status (true to ban, false to unban)", required = true) @RequestParam Boolean ban,
            @Parameter(description = "Ban reason") @RequestParam(required = false) String reason) {
        try {
            com.ween.dto.response.UserResponse response = adminService.banUnbanUser(id, ban, reason);
            return ResponseEntity.ok(ApiResponse.ok(response,
                    ban ? "User banned successfully" : "User unbanned successfully"));
        } catch (Exception e) {
            log.error("Failed to update ban status for user: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/organizations")
    @Operation(summary = "Get all organizations", description = "Retrieve list of all organizations on platform (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organizations retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponse<Page<OrganizationResponse>>> getAllOrganizations(
            @Parameter(description = "Filter by name") @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            Page<OrganizationResponse> response = adminService.getAllOrganizations(search, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Organizations retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve organizations", e);
            throw e;
        }
    }

    @PutMapping("/organizations/{id}/verify")
    @Transactional
    @Operation(summary = "Verify organization", description = "Verify or revoke verification for an organization (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization verification status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> verifyOrganization(
            @Parameter(description = "Organization ID", required = true) @PathVariable String id,
            @Parameter(description = "Verify status (true to verify, false to revoke)", required = true) @RequestParam Boolean verify,
            @Parameter(description = "Verification reason/note") @RequestParam(required = false) String note) {
        try {
            OrganizationResponse response = adminService.verifyOrganization(id, verify, note);
            return ResponseEntity.ok(ApiResponse.ok(response,
                    verify ? "Organization verified successfully" : "Organization verification revoke"));
        } catch (Exception e) {
            log.error("Failed to verify organization: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform statistics", description = "Get comprehensive platform statistics and metrics (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getPlatformStatistics() {
        try {
            AdminStatsResponse response = adminService.getPlatformStatistics();
            return ResponseEntity.ok(ApiResponse.ok(response, "Statistics retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve platform statistics", e);
            throw e;
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return (String) authentication.getPrincipal();
    }
}

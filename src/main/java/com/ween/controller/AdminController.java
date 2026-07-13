package com.ween.controller;

import com.ween.dto.request.AdjustCoinsRequest;
import com.ween.dto.request.BadgeRequest;
import com.ween.dto.request.ChangeRoleRequest;
import com.ween.dto.request.UpdateEventRequest;
import com.ween.dto.response.*;
import com.ween.entity.AuditLog;
import com.ween.service.AdminService;
import com.ween.service.BadgeService;
import jakarta.validation.Valid;
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
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Admin-only platform management endpoints")
public class AdminController {

    private final AdminService adminService;
    private final BadgeService badgeService;

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

    @PutMapping("/users/{id}/ban-user")
    @Transactional
    @Operation(summary = "Ban user", description = "Ban a user account (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User banned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Void>> banUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id,
            @Parameter(description = "Ban reason") @RequestParam(required = false) String reason) {
        try {
            adminService.banUser(id, reason, getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.ok(null, "User banned successfully"));
        } catch (Exception e) {
            log.error("Failed to ban user: {}", id, e);
            throw e;
        }
    }

    @PutMapping("/users/{id}/unban-user")
    @Transactional
    @Operation(summary = "Unban user", description = "Unban a user account (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User unbanned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Void>> unbanUser(
            @Parameter(description = "User ID", required = true) @PathVariable String id) {
        try {
            adminService.unbanUser(id, getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.ok(null, "User unbanned successfully"));
        } catch (Exception e) {
            log.error("Failed to unban user: {}", id, e);
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
            OrganizationResponse response = adminService.verifyOrganization(id, verify, note, getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.ok(response,
                    verify ? "Organization verified successfully" : "Organization verification revoke"));
        } catch (Exception e) {
            log.error("Failed to verify organization: {}", id, e);
            throw e;
        }
    }

    @PutMapping("/organizations/{id}/reject")
    @Transactional
    @Operation(summary = "Reject organization", description = "Reject an organization (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization rejected successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<Void>> rejectOrganization(
            @Parameter(description = "Organization ID", required = true) @PathVariable String id,
            @Parameter(description = "Rejection reason") @RequestParam(required = false) String reason) {
        try {
            adminService.rejectOrganization(id, reason, getCurrentUserId());
            return ResponseEntity.ok(ApiResponse.ok(null, "Organization rejected successfully"));
        } catch (Exception e) {
            log.error("Failed to reject organization: {}", id, e);
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

    @GetMapping("/badges")
    @Operation(summary = "List badge achievement rules")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<BadgeResponse>>> getBadges(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(badgeService.list(pageable), "Badges retrieved successfully"));
    }

    @PostMapping(value = "/badges", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create an achievement badge")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<BadgeResponse>> createBadge(
            @Valid @org.springframework.web.bind.annotation.RequestPart("request") BadgeRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(badgeService.create(request, image), "Badge created successfully"));
    }

    @PutMapping(value = "/badges/{badgeId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update an achievement badge")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<BadgeResponse>> updateBadge(
            @PathVariable String badgeId,
            @Valid @org.springframework.web.bind.annotation.RequestPart("request") BadgeRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.ok(badgeService.update(badgeId, request, image), "Badge updated successfully"));
    }

    @DeleteMapping("/badges/{badgeId}")
    @Operation(summary = "Deactivate an achievement badge")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> deactivateBadge(@PathVariable String badgeId) {
        badgeService.deactivate(badgeId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Badge deactivated successfully"));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user details", description = "Get comprehensive details about a user (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetails(@PathVariable String id) {
        AdminUserDetailResponse response = adminService.getUserDetails(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "User details retrieved successfully"));
    }

    @PutMapping("/users/{id}/role")
    @Transactional
    @Operation(summary = "Change user role", description = "Change a user's role (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable String id,
            @Valid @RequestBody ChangeRoleRequest request) {
        UserResponse response = adminService.changeUserRole(id, request.getRole(), getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "User role changed successfully"));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    @Operation(summary = "Delete user", description = "Permanently delete a user account and related records (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "User deleted successfully"));
    }

    @DeleteMapping("/organizations/{id}")
    @Transactional
    @Operation(summary = "Delete organization", description = "Permanently delete an organization and related records (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable String id) {
        adminService.deleteOrganization(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Organization deleted successfully"));
    }

    @PostMapping("/coins/adjust")
    @Transactional
    @Operation(summary = "Adjust coins balance", description = "Manually adjust a user's WeenCoin balance (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> adjustCoins(
            @Valid @RequestBody AdjustCoinsRequest request) {
        adminService.adjustCoins(request, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Coins adjusted successfully"));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs", description = "Get paginated list of all platform audit logs (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @PageableDefault(size = 50) Pageable pageable) {
        Page<AuditLog> response = adminService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Audit logs retrieved successfully"));
    }

    @GetMapping("/events")
    @Operation(summary = "Get all events", description = "Get paginated list of all events (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getAllEvents(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<EventResponse> response = adminService.getAllEvents(search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Events retrieved successfully"));
    }

    @PutMapping("/events/{id}")
    @Transactional
    @Operation(summary = "Update event", description = "Update details of any event (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventRequest request) {
        EventResponse response = adminService.updateEvent(id, request, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Event updated successfully"));
    }

    @DeleteMapping("/events/{id}")
    @Transactional
    @Operation(summary = "Delete event", description = "Delete any event (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable String id) {
        adminService.deleteEvent(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Event deleted successfully"));
    }

    @GetMapping("/events/{id}/registrations")
    @Operation(summary = "Get event registrations", description = "Get list of registered participants for an event (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<ParticipantResponse>>> getEventRegistrations(
            @PathVariable String id,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<ParticipantResponse> response = adminService.getEventRegistrations(id, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Event registrations retrieved successfully"));
    }

    @GetMapping("/posts")
    @Operation(summary = "Get all posts", description = "Get paginated list of all posts (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getAllPosts(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<PostResponse> response = adminService.getAllPosts(search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Posts retrieved successfully"));
    }

    @DeleteMapping("/posts/{id}")
    @Transactional
    @Operation(summary = "Delete post", description = "Delete any post (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable String id) {
        adminService.deletePost(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Post deleted successfully"));
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get post comments", description = "Get all comments for a post (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<PostCommentResponse>>> getPostComments(
            @PathVariable String postId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<PostCommentResponse> response = adminService.getPostComments(postId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Comments retrieved successfully"));
    }

    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    @Transactional
    @Operation(summary = "Delete comment", description = "Delete any comment (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId) {
        adminService.deleteComment(commentId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Comment deleted successfully"));
    }

    @GetMapping("/certificates")
    @Operation(summary = "Get all certificates", description = "Get paginated list of all certificates (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<CertificateResponse>>> getAllCertificates(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<CertificateResponse> response = adminService.getAllCertificates(search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Certificates retrieved successfully"));
    }

    @DeleteMapping("/certificates/{id}")
    @Transactional
    @Operation(summary = "Revoke certificate", description = "Revoke any certificate (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> revokeCertificate(@PathVariable String id) {
        adminService.revokeCertificate(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Certificate revoked successfully"));
    }

    @GetMapping("/referrals")
    @Operation(summary = "Get platform referrals", description = "Get all referrals (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<ReferralResponse>>> getReferralStats(
            @PageableDefault(size = 50) Pageable pageable) {
        Page<ReferralResponse> response = adminService.getReferralStats(pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Referrals retrieved successfully"));
    }

    @GetMapping("/ai/stats")
    @Operation(summary = "Get AI assistant statistics", description = "Get AI assistant statistics (ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<AiStatsResponse>> getAiStats() {
        AiStatsResponse response = adminService.getAiStats();
        return ResponseEntity.ok(ApiResponse.ok(response, "AI statistics retrieved successfully"));
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return (String) authentication.getPrincipal();
    }
}

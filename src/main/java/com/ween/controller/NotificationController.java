package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.NotificationResponse;
import com.ween.entity.Notification;
import com.ween.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get notifications", description = "Retrieve pageable list of user's notifications")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            String userId = getCurrentUserId();
            Page<NotificationResponse> response = notificationService.getUserNotificationsMapped(userId, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Notifications retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve notifications for user: {}", getCurrentUserId(), e);
            throw e;
        }
    }

    @PutMapping("/{id}/read")
    @Transactional
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Notification does not belong to user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found")
    })
    public ResponseEntity<ApiResponse<Notification>> markAsRead(
            @Parameter(description = "Notification ID", required = true) @PathVariable String id) {
        try {
            String userId = getCurrentUserId();
            Notification response = notificationService.markAsRead(userId, id);
            return ResponseEntity.ok(ApiResponse.ok(response, "Notification marked as read"));
        } catch (Exception e) {
            log.error("Failed to mark notification as read: {}", id, e);
            throw e;
        }
    }

    @PutMapping("/read-all")
    @Transactional
    @Operation(summary = "Mark all notifications as read", description = "Mark all user's notifications as read")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        try {
            String userId = getCurrentUserId();
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "All notifications marked as read"));
        } catch (Exception e) {
            log.error("Failed to mark all notifications as read for user: {}", getCurrentUserId(), e);
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

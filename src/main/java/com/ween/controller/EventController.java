package com.ween.controller;

import com.ween.dto.request.CreateEventRequest;
import com.ween.dto.request.UpdateEventRequest;
import com.ween.dto.response.*;
import com.ween.entity.Event;
import com.ween.enums.EventCategory;
import com.ween.enums.EventStatus;
import com.ween.security.SecurityUtil;
import com.ween.exception.UnauthorizedException;
import com.ween.service.EventService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @Operation(summary = "List events", description = "Retrieve list of events with optional filters and pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Events retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<EventResponse>>> listEvents(
            @Parameter(description = "Event category filter") @RequestParam(required = false) EventCategory category,
            @Parameter(description = "City filter") @RequestParam(required = false) String city,
            @Parameter(description = "Date from filter") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @Parameter(description = "Date to filter") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @Parameter(description = "Search text") @RequestParam(required = false) String search,
            @Parameter(description = "Organization ID filter") @RequestParam(required = false) String organizationId,
            @Parameter(description = "Sort field (default: createdAt)") @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            Page<EventResponse> response = eventService.listEvents(category, city, dateFrom, dateTo,
                    search, organizationId, sort, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Events retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to list events", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event details", description = "Retrieve detailed information about a specific event")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEventDetail(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            EventDetailResponse response = eventService.getEventDetail(id);
            return ResponseEntity.ok(ApiResponse.ok(response, "Event retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve event: {}", id, e);
            throw e;
        }
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    @Operation(summary = "Create event", description = "Create a new event (ORGANIZER only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Event created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<Event>> createEvent(
            @io.swagger.v3.oas.annotations.Parameter(content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json")) @Valid @org.springframework.web.bind.annotation.RequestPart("request") CreateEventRequest request,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage) {
        try {
            String orgId = securityUtil.getCurrentUserId();
            Event response = eventService.createEvent(request, orgId, coverImage);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Event created successfully"));
        } catch (Exception e) {
            log.error("Failed to create event for user: {}", securityUtil.getCurrentUserId(), e);
            throw e;
        }
    }

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    @Operation(summary = "Update event", description = "Update event details (ORGANIZER only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<ApiResponse<Event>> updateEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable String id,
            @io.swagger.v3.oas.annotations.Parameter(content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json")) @Valid @org.springframework.web.bind.annotation.RequestPart("request") UpdateEventRequest request,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage) {
        try {
            String userId = securityUtil.getCurrentUserId();
            Event response = eventService.updateEvent(id, userId, request, coverImage);
            return ResponseEntity.ok(ApiResponse.ok(response, "Event updated successfully"));
        } catch (Exception e) {
            log.error("Failed to update event: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Delete event", description = "Cancel/Delete an event (ORGANIZER only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            String userId = securityUtil.getCurrentUserId();
            eventService.deleteEventData(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Event deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete event: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/publish")
    @Transactional
    @Operation(summary = "Publish event", description = "Publish a draft event (ORGANIZER/ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> publishEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            String userId = securityUtil.getCurrentUserId();
            eventService.publishEvent(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Event published successfully"));
        } catch (Exception e) {
            log.error("Failed to publish event: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/start")
    @Transactional
    @Operation(summary = "Start event", description = "Mark event as ongoing (ORGANIZER/ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> startEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            String userId = securityUtil.getCurrentUserId();
            eventService.startEvent(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Event started successfully"));
        } catch (Exception e) {
            log.error("Failed to start event: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/complete")
    @Transactional
    @Operation(summary = "Complete event", description = "Mark event as completed (ORGANIZER/ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> completeEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            String userId = securityUtil.getCurrentUserId();
            eventService.completeEvent(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Event completed successfully"));
        } catch (Exception e) {
            log.error("Failed to complete event: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    @Operation(summary = "Cancel event", description = "Cancel an event (ORGANIZER/ADMIN only)")
    @SecurityRequirement(name = "Bearer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZATION_ADMIN', 'ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> cancelEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            String userId = securityUtil.getCurrentUserId();
            eventService.cancelEvent(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Event cancelled successfully"));
        } catch (Exception e) {
            log.error("Failed to cancel event: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get event statistics", description = "Get event attendance analytics (ORGANIZER only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<ApiResponse<EventStatsResponse>> getEventStats(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            String userId = securityUtil.getCurrentUserId();
            EventStatsResponse response = eventService.getEventStats(userId, id);
            return ResponseEntity.ok(ApiResponse.ok(response, "Statistics retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve stats for event: {}", id, e);
            throw e;
        }
    }

}

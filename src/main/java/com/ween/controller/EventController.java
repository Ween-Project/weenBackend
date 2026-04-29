package com.ween.controller;

import com.ween.dto.request.CreateEventRequest;
import com.ween.dto.request.UpdateEventRequest;
import com.ween.dto.response.*;
import com.ween.entity.Event;
import com.ween.entity.EventRegistration;
import com.ween.enums.EventCategory;
import com.ween.mapper.EventMapper;
import com.ween.exception.UnauthorizedException;
import com.ween.service.EventService;
import com.ween.service.RegistrationService;
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
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final EventMapper eventMapper;

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

    @PostMapping
    @Transactional
    @Operation(summary = "Create event", description = "Create a new event (ORGANIZER only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Event created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<Event>> createEvent(
            @Valid @RequestBody CreateEventRequest request) {
        try {
            String orgId = getCurrentUserId();
            Event response = eventService.createEvent(request, orgId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Event created successfully"));
        } catch (Exception e) {
            log.error("Failed to create event for user: {}", getCurrentUserId(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
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
            @Valid @RequestBody UpdateEventRequest request) {
        try {
            String userId = getCurrentUserId();
            Event response = eventService.updateEvent(id, userId, request);
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
            String userId = getCurrentUserId();
            eventService.cancelEvent(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Event deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete event: {}", id, e);
            throw e;
        }
    }

    @PostMapping("/{id}/register")
    @Transactional
    @Operation(summary = "Register for event", description = "Register user for an event (VOLUNTEER)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Already registered or event capacity exceeded")
    })
    public ResponseEntity<ApiResponse<EventRegistration>> registerForEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            String userId = getCurrentUserId();
            EventRegistration response = registrationService.registerForEvent(id, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Registered successfully"));
        } catch (Exception e) {
            log.error("Failed to register for event: {}", id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}/register")
    @Transactional
    @Operation(summary = "Cancel event registration", description = "Cancel user's registration for an event")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<ApiResponse<Void>> cancelEventRegistration(
            @Parameter(description = "Event ID", required = true) @PathVariable String id) {
        try {
            String userId = getCurrentUserId();
            registrationService.cancelRegistration(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(null, "Registration cancelled successfully"));
        } catch (Exception e) {
            log.error("Failed to cancel registration for event: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Get event participants", description = "Get list of event participants (ORGANIZER only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Participants retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<ApiResponse<Page<ParticipantResponse>>> getEventParticipants(
            @Parameter(description = "Event ID", required = true) @PathVariable String id,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            String userId = getCurrentUserId();
            Page<ParticipantResponse> response = registrationService.getEventParticipants(userId, id, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Participants retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve participants for event: {}", id, e);
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
            String userId = getCurrentUserId();
            EventStatsResponse response = eventService.getEventStats(userId, id);
            return ResponseEntity.ok(ApiResponse.ok(response, "Statistics retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve stats for event: {}", id, e);
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

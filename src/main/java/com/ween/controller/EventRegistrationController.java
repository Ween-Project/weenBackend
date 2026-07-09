package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.ParticipantResponse;
import com.ween.entity.EventRegistration;
import com.ween.security.SecurityUtil;
import com.ween.service.ParticipationService;
import com.ween.service.RegistrationService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event Registrations", description = "Endpoints for event registration and participant management")
public class EventRegistrationController {

    private final RegistrationService registrationService;
    private final ParticipationService participationService;
    private final SecurityUtil securityUtil;

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
            String userId = securityUtil.getCurrentUserId();
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
            String userId = securityUtil.getCurrentUserId();
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
            String userId = securityUtil.getCurrentUserId();
            Page<ParticipantResponse> response = registrationService.getEventParticipants(userId, id, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Participants retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve participants for event: {}", id, e);
            throw e;
        }
    }
}

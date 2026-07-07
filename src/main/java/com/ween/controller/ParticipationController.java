package com.ween.controller;

import com.ween.dto.request.CheckinRequest;
import com.ween.dto.response.CheckinResponse;
import com.ween.security.SecurityUtil;
import com.ween.service.ParticipationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/participations")
@RequiredArgsConstructor
@Tag(name = "Participations")
public class ParticipationController {

    private final ParticipationService participationService;
    private final SecurityUtil securityUtil;


    @PostMapping("/checkin-join")
    @io.swagger.v3.oas.annotations.Operation(summary = "Check-in and join to event", description = "Check-in participant to an event using QR token")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer")
    public ResponseEntity<com.ween.dto.response.ApiResponse<com.ween.dto.response.CheckinResponse>> checkin(
            @Valid @RequestBody CheckinRequest request) {
        try {
          CheckinResponse response = participationService.checkinViaQr(request.getEventId(), request.getQrToken());
            return ResponseEntity.ok(com.ween.dto.response.ApiResponse.ok(response, "Check-in successful"));
        } catch (Exception e) {
            log.error("Failed to check-in participant for event: {}", request.getEventId(), e);
            throw e;
        }
    }


    @PostMapping("/complete/{eventId}")
    public ResponseEntity<String> completeParticipation(@PathVariable String eventId) {
        String currentUserId = securityUtil.getCurrentUserId();

        participationService.completeParticipation(currentUserId, eventId);

        return ResponseEntity.ok("Participation completed and certificate generated.");
    }
}

package com.ween.controller;

import com.ween.dto.request.CheckinRequest;
import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.CheckinResponse;
import com.ween.service.QrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
@Tag(name = "QR Code", description = "QR code and check-in endpoints")
public class QrController {

    private final QrService qrService;

    @PostMapping("/checkin")
    @Transactional
    @Operation(summary = "Check-in to event", description = "Check-in participant to an event using QR token")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check-in successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid QR token or invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event or participant not found")
    })
    public ResponseEntity<ApiResponse<CheckinResponse>> checkin(
            @Valid @RequestBody CheckinRequest request) {
        try {
            CheckinResponse response = qrService.checkinParticipant(request.getEventId(), request.getQrToken());
            return ResponseEntity.ok(ApiResponse.ok(response, "Check-in successful"));
        } catch (Exception e) {
            log.error("Failed to check-in participant for event: {}", request.getEventId(), e);
            throw e;
        }
    }
}
package com.ween.controller;

import com.ween.dto.request.CheckinRequest;
import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.CheckinResponse;
import com.ween.security.SecurityUtil;
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
    private final SecurityUtil securityUtil;


    @GetMapping("/generate")
    @Operation(summary = "Generate QR token", description = "Generates a 30-second expiring QR token for the current user")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<com.ween.dto.response.QrResponse>> generateQr() {
        try {
            String currentUserId = securityUtil.getCurrentUserId();
            String token = qrService.generateQrToken(currentUserId);
            com.ween.dto.response.QrResponse qrResponse = com.ween.dto.response.QrResponse.builder()
                    .encryptedPayload(token)
                    .expiresIn(30L)
                    .build();
            return ResponseEntity.ok(ApiResponse.ok(qrResponse, "Token generated successfully"));
        } catch (Exception e) {
            log.error("Failed to generate QR token", e);
            throw e;
        }
    }
}
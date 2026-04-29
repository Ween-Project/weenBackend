package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.entity.Certificate;
import com.ween.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificates", description = "Certificate management and verification endpoints")
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/generate/{eventId}")
    @Transactional
    @Operation(summary = "Generate certificates", description = "Batch generate certificates for event participants (ORGANIZER only, async)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Certificate generation started"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found")
    })
    public ResponseEntity<ApiResponse<String>> generateCertificates(
            @Parameter(description = "Event ID", required = true) @PathVariable String eventId) {
        try {
            String userId = getCurrentUserId();
            String taskId = certificateService.generateCertificatesAsync(userId, eventId);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.ok(taskId, "Certificate generation started"));
        } catch (Exception e) {
            log.error("Failed to generate certificates for event: {}", eventId, e);
            throw e;
        }
    }

    @GetMapping("/verify/{certNumber}")
    @Operation(summary = "Verify certificate", description = "Publicly verify a certificate by certificate number")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Certificate verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    public ResponseEntity<ApiResponse<Boolean>> verifyCertificate(
            @Parameter(description = "Certificate number", required = true) @PathVariable String certNumber) {
        try {
            boolean response = certificateService.verifyCertificate(certNumber);
            return ResponseEntity.ok(ApiResponse.ok(response, "Certificate verified successfully"));
        } catch (Exception e) {
            log.error("Failed to verify certificate: {}", certNumber, e);
            throw e;
        }
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download certificate PDF", description = "Download certificate as PDF file")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF downloaded successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Certificate does not belong to user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Certificate not found")
    })
    public ResponseEntity<byte[]> downloadCertificatePdf(
            @Parameter(description = "Certificate ID", required = true) @PathVariable String id) {
        try {
            String userId = getCurrentUserId();
            byte[] pdfContent = certificateService.downloadCertificatePdf(userId, id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificate.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            log.error("Failed to download certificate: {}", id, e);
            throw e;
        }
    }

    @GetMapping("/my")
    @Operation(summary = "Get my certificates", description = "Retrieve list of user's earned certificates")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Certificates retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<Certificate>>> getMyCertificates(
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

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return (String) authentication.getPrincipal();
    }
}

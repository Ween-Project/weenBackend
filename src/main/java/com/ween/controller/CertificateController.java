package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.entity.Certificate;
import com.ween.security.SecurityUtil;
import com.ween.service.CertificateService;
import com.ween.repository.CertificateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name="Certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final CertificateRepository certificateRepository;
    private final SecurityUtil securityUtil;

    @GetMapping("/my")
    public ResponseEntity<List<Certificate>> getMyCertificates() {
        String userId = securityUtil.getCurrentUserId();

        List<Certificate> certificates = certificateRepository.findByUserId(userId);
        return ResponseEntity.ok(certificates);
    }

    /**
     * Generates and downloads the PDF for a specific certificate ID.
     */
    @GetMapping(value="/download/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String id) throws Exception {
        // Generate the PDF byte array using the service
        byte[] pdfContent = certificateService.createCertificatePdf(id);

        // Prepare HTTP headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        // This header tells the browser to treat the response as a downloadable file
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("certificate_" + id + ".pdf")
                .build());

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    @DeleteMapping("/{certificateId}")
    @Operation(summary = "Delete certificate")
    public ResponseEntity<ApiResponse<Void>> deleteCertificate(@PathVariable String certificateId) {
        certificateService.deleteCertificate(certificateId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Certificate deleted successfully"));
    }
}
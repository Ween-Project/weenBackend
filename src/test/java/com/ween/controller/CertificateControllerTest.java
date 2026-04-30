package com.ween.controller;

import com.ween.entity.Certificate;
import com.ween.security.JwtUtil;
import com.ween.service.CertificateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CertificateController.class)
@AutoConfigureMockMvc(addFilters = false)
class CertificateControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CertificateService certificateService;
    @MockBean private JwtUtil jwtUtil;
    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user-id", null, List.of())
        );
    }

    @Test @DisplayName("POST /api/v1/certificates/generate/{eventId} - generate certificates")
    void generateCertificates() throws Exception {
        when(certificateService.generateCertificatesAsync("test-user-id", "eid")).thenReturn("task123");

        mockMvc.perform(post("/api/v1/certificates/generate/eid"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data").value("task123"));
    }

    @Test @DisplayName("GET /api/v1/certificates/verify/{certNumber} - verify certificate")
    void verifyCertificate() throws Exception {
        when(certificateService.verifyCertificate("CERT-1")).thenReturn(true);

        mockMvc.perform(get("/api/v1/certificates/verify/CERT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test @DisplayName("GET /api/v1/certificates/{id}/download - download PDF")
    void downloadCertificatePdf() throws Exception {
        byte[] pdfData = new byte[]{1, 2, 3};
        when(certificateService.downloadCertificatePdf("test-user-id", "cid")).thenReturn(pdfData);

        mockMvc.perform(get("/api/v1/certificates/cid/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=certificate.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfData));
    }

    @Test @DisplayName("GET /api/v1/certificates/my - get my certificates")
    void getMyCertificates() throws Exception {
        when(certificateService.getUserCertificates("test-user-id")).thenReturn(List.of(
                Certificate.builder().certificateNumber("CERT-1").build()
        ));

        mockMvc.perform(get("/api/v1/certificates/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

package com.ween.controller;

import com.ween.entity.Certificate;
import com.ween.repository.CertificateRepository;
import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CertificateController.class)
@AutoConfigureMockMvc(addFilters = false)
class CertificateControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CertificateService certificateService;
    @MockBean private CertificateRepository certificateRepository;
    @MockBean private SecurityUtil securityUtil;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user-id", null, List.of())
        );
        when(securityUtil.getCurrentUserId()).thenReturn("test-user-id");
    }

    @Test @DisplayName("GET /api/v1/certificates/my - get my certificates")
    void getMyCertificates() throws Exception {
        when(certificateRepository.findByUserId("test-user-id")).thenReturn(List.of(
                Certificate.builder().certificateNumber("CERT-1").build()
        ));

        mockMvc.perform(get("/api/v1/certificates/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].certificateNumber").value("CERT-1"));
    }

    @Test @DisplayName("GET /api/v1/certificates/my - empty list")
    void getMyCertificates_empty() throws Exception {
        when(certificateRepository.findByUserId("test-user-id")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/certificates/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test @DisplayName("GET /api/v1/certificates/download/{id} - download PDF")
    void downloadCertificatePdf() throws Exception {
        byte[] pdfData = new byte[]{1, 2, 3};
        when(certificateService.createCertificatePdf("cid")).thenReturn(pdfData);

        mockMvc.perform(get("/api/v1/certificates/download/cid"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfData));
    }
}

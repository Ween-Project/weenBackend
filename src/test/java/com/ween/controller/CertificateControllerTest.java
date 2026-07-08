package com.ween.controller;

import com.ween.entity.Certificate;
import com.ween.repository.CertificateRepository;
import com.ween.security.SecurityUtil;
import com.ween.service.CertificateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CertificateControllerTest {

    private CertificateService certificateService;
    private CertificateRepository certificateRepository;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        certificateService = mock(CertificateService.class);
        certificateRepository = mock(CertificateRepository.class);
        securityUtil = mock(SecurityUtil.class);
        mockMvc = standaloneSetup(new CertificateController(certificateService, certificateRepository, securityUtil)).build();
    }

    @Test
    void getMyCertificatesUsesCurrentUser() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        when(certificateRepository.findByUserId("user-1")).thenReturn(List.of(new Certificate()));

        mockMvc.perform(get("/api/v1/certificates/my"))
                .andExpect(status().isOk());

        verify(certificateRepository).findByUserId("user-1");
    }

    @Test
    void downloadCertificateReturnsPdfAttachment() throws Exception {
        when(certificateService.createCertificatePdf("cert-1")).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/certificates/download/cert-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }
}

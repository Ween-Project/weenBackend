package com.ween.controller;

import com.ween.security.SecurityUtil;
import com.ween.service.QrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QrControllerTest {

    private QrService qrService;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        qrService = mock(QrService.class);
        securityUtil = mock(SecurityUtil.class);
        mockMvc = standaloneSetup(new QrController(qrService, securityUtil)).build();
    }

    @Test
    void generateQrReturnsEncryptedPayload() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        when(qrService.generateQrToken("user-1")).thenReturn("encrypted-token");

        mockMvc.perform(get("/api/v1/qr/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.encryptedPayload").value("encrypted-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(30));

        verify(qrService).generateQrToken("user-1");
    }
}

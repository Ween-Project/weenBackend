package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.CheckinRequest;
import com.ween.dto.response.CheckinResponse;
import com.ween.security.JwtUtil;
import com.ween.service.QrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QrController.class)
@AutoConfigureMockMvc(addFilters = false)
class QrControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private QrService qrService;
    @MockBean private JwtUtil jwtUtil;

    @Test @DisplayName("POST /api/v1/qr/checkin - checkin participant")
    void checkin() throws Exception {
        CheckinRequest req = new CheckinRequest();
        req.setEventId("eid"); req.setQrToken("encrypted");

        CheckinResponse res = CheckinResponse.builder().status("Success").build();
        when(qrService.checkinParticipant("eid", "encrypted")).thenReturn(res);

        mockMvc.perform(post("/api/v1/qr/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Success"));
    }
}

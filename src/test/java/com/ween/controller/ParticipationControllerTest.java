package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.CheckinRequest;
import com.ween.dto.response.CheckinResponse;
import com.ween.security.SecurityUtil;
import com.ween.service.ParticipationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParticipationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ParticipationService participationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        participationService = mock(ParticipationService.class);
        mockMvc = standaloneSetup(new ParticipationController(participationService)).build();
    }

    @Test
    void checkinDelegatesToParticipationService() throws Exception {
        when(participationService.checkinViaQr("event-1", "qr-token"))
                .thenReturn(CheckinResponse.builder().status("CHECKED_IN").participantName("Ali").build());

        mockMvc.perform(post("/api/v1/participations/checkin-join")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CheckinRequest("event-1", "qr-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.data.participantName").value("Ali"));
    }
}

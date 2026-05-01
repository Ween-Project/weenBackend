package com.ween.controller;

import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
import com.ween.service.ParticipationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ParticipationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParticipationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ParticipationService participationService;
    @MockBean private SecurityUtil securityUtil;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user-id", null, List.of())
        );
        when(securityUtil.getCurrentUserId()).thenReturn("test-user-id");
    }

    @Test @DisplayName("POST /api/v1/participations/join/{eventId} - join event")
    void joinEvent() throws Exception {
        doNothing().when(participationService).joinEvent("test-user-id", "eid");

        mockMvc.perform(post("/api/v1/participations/join/eid"))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully joined the event."));

        verify(participationService).joinEvent("test-user-id", "eid");
    }

    @Test @DisplayName("POST /api/v1/participations/complete/{eventId} - complete participation")
    void completeParticipation() throws Exception {
        doNothing().when(participationService).completeParticipation("test-user-id", "eid");

        mockMvc.perform(post("/api/v1/participations/complete/eid"))
                .andExpect(status().isOk())
                .andExpect(content().string("Participation completed and certificate generated."));

        verify(participationService).completeParticipation("test-user-id", "eid");
    }
}

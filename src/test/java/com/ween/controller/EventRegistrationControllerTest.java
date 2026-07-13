package com.ween.controller;

import com.ween.dto.response.ParticipantResponse;
import com.ween.entity.EventRegistration;
import com.ween.security.SecurityUtil;
import com.ween.service.ParticipationService;
import com.ween.service.RegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class EventRegistrationControllerTest {

    private RegistrationService registrationService;
    private ParticipationService participationService;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        participationService = mock(ParticipationService.class);
        securityUtil = mock(SecurityUtil.class);

        mockMvc = standaloneSetup(new EventRegistrationController(registrationService, participationService, securityUtil))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        ControllerTestSupport.authenticateAs("user-1");
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void registerForEventCreatesRegistration() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        EventRegistration registration = mock(EventRegistration.class);
        when(registrationService.registerForEvent("event-123", "user-1")).thenReturn(registration);

        mockMvc.perform(post("/api/v1/events/event-123/register"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registered successfully"));

        verify(registrationService).registerForEvent("event-123", "user-1");
    }

    @Test
    void cancelEventRegistrationRemovesRegistration() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");

        mockMvc.perform(delete("/api/v1/events/event-123/register"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration cancelled successfully"));

        verify(registrationService).cancelRegistration("event-123", "user-1");
    }

    @Test
    void getEventParticipantsReturnsPage() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        ParticipantResponse participant = mock(ParticipantResponse.class);
        when(registrationService.getEventParticipants(eq("user-1"), eq("event-123"), any()))
                .thenReturn(new PageImpl<>(List.of(participant), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/api/v1/events/event-123/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Participants retrieved successfully"));
    }
}

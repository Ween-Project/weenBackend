package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.CreateEventRequest;
import com.ween.dto.request.UpdateEventRequest;
import com.ween.dto.response.*;
import com.ween.entity.Event;
import com.ween.entity.EventRegistration;
import com.ween.enums.EventCategory;
import com.ween.mapper.EventMapper;
import com.ween.security.JwtUtil;
import com.ween.service.EventService;
import com.ween.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private EventService eventService;
    @MockBean private RegistrationService registrationService;
    @MockBean private EventMapper eventMapper;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user-id", null, List.of())
        );
    }

    @Test @DisplayName("GET /api/v1/events - list events")
    void listEvents() throws Exception {
        Page<EventResponse> page = new PageImpl<>(List.of(new EventResponse()));
        when(eventService.listEvents(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/events?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test @DisplayName("GET /api/v1/events/{id} - get detail")
    void getEventDetail() throws Exception {
        EventDetailResponse res = new EventDetailResponse();
        when(eventService.getEventDetail("eid")).thenReturn(res);

        mockMvc.perform(get("/api/v1/events/eid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test @DisplayName("POST /api/v1/events - create event")
    void createEvent() throws Exception {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("T");
        req.setDescription("D");
        req.setStartDate(LocalDateTime.now().plusDays(1));
        req.setEndDate(LocalDateTime.now().plusDays(2));
        req.setCategory(EventCategory.EDUCATION);
        req.setIsOnline(false);

        Event res = Event.builder().title("T").build();
        when(eventService.createEvent(any(), eq("test-user-id"))).thenReturn(res);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test @DisplayName("PUT /api/v1/events/{id} - update event")
    void updateEvent() throws Exception {
        UpdateEventRequest req = new UpdateEventRequest();
        Event res = Event.builder().build();

        when(eventService.updateEvent(eq("eid"), eq("test-user-id"), any())).thenReturn(res);

        mockMvc.perform(put("/api/v1/events/eid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("DELETE /api/v1/events/{id} - delete event")
    void deleteEvent() throws Exception {
        mockMvc.perform(delete("/api/v1/events/eid"))
                .andExpect(status().isOk());
        verify(eventService).cancelEvent("eid", "test-user-id");
    }

    @Test @DisplayName("POST /api/v1/events/{id}/register - register")
    void registerForEvent() throws Exception {
        EventRegistration res = EventRegistration.builder().build();
        when(registrationService.registerForEvent("eid", "test-user-id")).thenReturn(res);

        mockMvc.perform(post("/api/v1/events/eid/register"))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("DELETE /api/v1/events/{id}/register - cancel registration")
    void cancelRegistration() throws Exception {
        mockMvc.perform(delete("/api/v1/events/eid/register"))
                .andExpect(status().isOk());
        verify(registrationService).cancelRegistration("eid", "test-user-id");
    }

    @Test @DisplayName("GET /api/v1/events/{id}/participants")
    void getEventParticipants() throws Exception {
        Page<ParticipantResponse> page = new PageImpl<>(List.of());
        when(registrationService.getEventParticipants(eq("test-user-id"), eq("eid"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/events/eid/participants"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("GET /api/v1/events/{id}/stats")
    void getEventStats() throws Exception {
        EventStatsResponse res = new EventStatsResponse();
        when(eventService.getEventStats("test-user-id", "eid")).thenReturn(res);

        mockMvc.perform(get("/api/v1/events/eid/stats"))
                .andExpect(status().isOk());
    }
}

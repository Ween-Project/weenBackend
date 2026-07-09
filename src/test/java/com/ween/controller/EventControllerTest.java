package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ween.dto.request.CreateEventRequest;
import com.ween.dto.response.EventDetailResponse;
import com.ween.dto.response.EventResponse;
import com.ween.entity.Event;
import com.ween.entity.EventRegistration;
import com.ween.enums.EventCategory;
import com.ween.mapper.EventMapper;
import com.ween.service.EventService;
import com.ween.service.RegistrationService;
import com.ween.service.ParticipationService;
import com.ween.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private EventService eventService;
    private RegistrationService registrationService;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        registrationService = mock(RegistrationService.class);
        securityUtil = mock(SecurityUtil.class);
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        mockMvc = standaloneSetup(
                new EventController(eventService, securityUtil),
                new EventRegistrationController(registrationService, mock(ParticipationService.class), securityUtil)
        )
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void listEventsReturnsPagedResponse() throws Exception {
        EventResponse response = EventResponse.builder().id("event-1").title("Cleanup").city("Baku").build();
        when(eventService.listEvents(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/events").param("search", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("event-1"));
    }

    @Test
    void getEventDetailDelegatesToService() throws Exception {
        when(eventService.getEventDetail("event-1"))
                .thenReturn(EventDetailResponse.builder().id("event-1").title("Cleanup").build());

        mockMvc.perform(get("/api/v1/events/event-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Cleanup"));
    }

    @Test
    void createEventUsesAuthenticatedOrganizationId() throws Exception {
        ControllerTestSupport.authenticateAs("org-1");
        when(securityUtil.getCurrentUserId()).thenReturn("org-1");
        Event event = Event.builder().title("Cleanup").organizationId("org-1").build();
        event.setId("event-1");
        when(eventService.createEvent(any(CreateEventRequest.class), eq("org-1"))).thenReturn(event);
        CreateEventRequest request = new CreateEventRequest(
                "Cleanup",
                "Community cleanup day",
                EventCategory.ENVIRONMENT,
                "Baku",
                "Nizami street",
                false,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                LocalDateTime.now().plusHours(12),
                25,
                null,
                null);

        mockMvc.perform(post("/api/v1/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Event created successfully"))
                .andExpect(jsonPath("$.data.id").value("event-1"));
    }

    @Test
    void registerForEventUsesAuthenticatedUserId() throws Exception {
        ControllerTestSupport.authenticateAs("user-1");
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        EventRegistration registration = EventRegistration.builder().eventId("event-1").userId("user-1").build();
        registration.setId("registration-1");
        when(registrationService.registerForEvent("event-1", "user-1")).thenReturn(registration);

        mockMvc.perform(post("/api/v1/events/event-1/register"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registered successfully"));

        verify(registrationService).registerForEvent("event-1", "user-1");
    }
}

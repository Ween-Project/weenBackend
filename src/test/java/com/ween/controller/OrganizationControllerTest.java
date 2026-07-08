package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.UpdateProfilePhotoRequest;
import com.ween.entity.Organization;
import com.ween.security.SecurityUtil;
import com.ween.service.EventService;
import com.ween.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OrganizationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SecurityUtil securityUtil;
    private EventService eventService;
    private OrganizationService organizationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        securityUtil = mock(SecurityUtil.class);
        eventService = mock(EventService.class);
        organizationService = mock(OrganizationService.class);
        mockMvc = standaloneSetup(new OrganizationController(securityUtil, eventService, organizationService)).build();
    }

    @Test
    void getOrganizationUsesAuthenticatedOrganization() throws Exception {
        Organization organization = Organization.builder().username("org").build();
        organization.setId("org-1");
        when(securityUtil.getCurrentUserId()).thenReturn("org-1");
        when(organizationService.getOrganizationById("org-1")).thenReturn(organization);

        mockMvc.perform(get("/api/v1/organizations/org-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("org"));
    }

    @Test
    void updateLogoDelegatesToService() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("org-1");
        when(organizationService.updateOrganizationPhoto(any(), any())).thenReturn(Organization.builder().logoUrl("logo.png").build());

        UpdateProfilePhotoRequest request = new UpdateProfilePhotoRequest();
        request.setImageUrl("logo.png");
        mockMvc.perform(put("/api/v1/organizations/org-1/logo")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Organization logo updated successfully"));
    }

    @Test
    void getOrganizationEventsUsesCurrentOrganization() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("org-1");
        when(eventService.getOrganizationEventsList("org-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/organizations/current-organization-events"))
                .andExpect(status().isOk());

        verify(eventService).getOrganizationEventsList("org-1");
    }
}

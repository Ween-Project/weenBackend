package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.UpdateOrganizationRequest;
import com.ween.dto.request.UpdateProfilePhotoRequest;
import com.ween.entity.Organization;
import com.ween.security.JwtUtil;
import com.ween.security.SecurityUtil;
import com.ween.service.EventService;
import com.ween.service.OrganizationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrganizationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OrganizationService organizationService;
    @MockBean private EventService eventService;
    @MockBean private SecurityUtil securityUtil;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        when(securityUtil.getCurrentUserId()).thenReturn("org-id");
    }

    @Test @DisplayName("GET /api/v1/organizations/{id} - get organization")
    void getProfile() throws Exception {
        Organization org = Organization.builder().organizationName("Org").build();
        when(organizationService.getOrganizationById("org-id")).thenReturn(org);

        mockMvc.perform(get("/api/v1/organizations/org-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationName").value("Org"));
    }

    @Test @DisplayName("PUT /api/v1/organizations/{id} - update organization")
    void updateProfile() throws Exception {
        UpdateOrganizationRequest req = new UpdateOrganizationRequest();
        req.setName("Updated Org");

        Organization org = Organization.builder().organizationName("Updated Org").build();
        when(organizationService.updateOrganization(eq("org-id"), any())).thenReturn(org);

        mockMvc.perform(put("/api/v1/organizations/org-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organizationName").value("Updated Org"));
    }

    @Test @DisplayName("PUT /api/v1/organizations/{id}/logo - update logo")
    void updatePhoto() throws Exception {
        UpdateProfilePhotoRequest req = new UpdateProfilePhotoRequest();
        req.setImageUrl("http://logo");

        Organization org = Organization.builder().logoUrl("http://logo").build();
        when(organizationService.updateOrganizationPhoto(eq("org-id"), any())).thenReturn(org);

        mockMvc.perform(put("/api/v1/organizations/org-id/logo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.logoUrl").value("http://logo"));
    }

    @Test @DisplayName("GET /api/v1/organizations/current-organization-events")
    void getOrganizationEvents() throws Exception {
        when(eventService.getOrganizationEventsList("org-id")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/organizations/current-organization-events"))
                .andExpect(status().isOk());
    }
}
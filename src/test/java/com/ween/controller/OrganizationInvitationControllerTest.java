package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.InviteOrganizerRequest;
import com.ween.service.OrganizationInvitationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OrganizationInvitationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OrganizationInvitationService invitationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        invitationService = mock(OrganizationInvitationService.class);
        mockMvc = standaloneSetup(new OrganizationInvitationController(invitationService)).build();
        ControllerTestSupport.authenticateAs("user-1");
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void inviteOrganizerDelegatesToService() throws Exception {
        InviteOrganizerRequest request = new InviteOrganizerRequest("targetuser");

        mockMvc.perform(post("/api/v1/organizations/org-123/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation sent successfully"));

        verify(invitationService).inviteOrganizer("org-123", request);
    }

    @Test
    void approveInvitationDelegatesToService() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/approve").param("token", "token-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation approved successfully. You are now an organizer."));

        verify(invitationService).approveInvitation("token-abc");
    }

    @Test
    void rejectInvitationDelegatesToService() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/reject").param("token", "token-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation rejected successfully."));

        verify(invitationService).rejectInvitation("token-xyz");
    }

    @Test
    void removeOrganizerDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/organizations/org-123/organizers/organizer-456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Organizer removed successfully"));

        verify(invitationService).removeOrganizer("org-123", "organizer-456");
    }
}

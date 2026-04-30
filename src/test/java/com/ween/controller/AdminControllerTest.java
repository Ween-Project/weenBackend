package com.ween.controller;

import com.ween.dto.response.AdminStatsResponse;
import com.ween.dto.response.OrganizationResponse;
import com.ween.entity.Organization;
import com.ween.entity.User;
import com.ween.security.JwtUtil;
import com.ween.service.AdminService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AdminService adminService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin-id", null, List.of())
        );
    }

    @Test @DisplayName("GET /api/v1/admin/users - get all users")
    void getAllUsers() throws Exception {
        Page<com.ween.dto.response.UserResponse> page = new PageImpl<>(List.of());
        when(adminService.getAllUsers(eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("PUT /api/v1/admin/users/{id}/ban - ban user")
    void banUser() throws Exception {
        when(adminService.banUnbanUser(eq("uid"), eq(true), eq("Spam")))
                .thenReturn(new com.ween.dto.response.UserResponse());

        mockMvc.perform(put("/api/v1/admin/users/uid/ban")
                        .param("ban", "true")
                        .param("reason", "Spam"))
                .andExpect(status().isOk());

        verify(adminService).banUnbanUser("uid", true, "Spam");
    }

    @Test @DisplayName("PUT /api/v1/admin/users/{id}/ban - unban user")
    void unbanUser() throws Exception {
        when(adminService.banUnbanUser(eq("uid"), eq(false), eq(null)))
                .thenReturn(new com.ween.dto.response.UserResponse());

        mockMvc.perform(put("/api/v1/admin/users/uid/ban")
                        .param("ban", "false"))
                .andExpect(status().isOk());

        verify(adminService).banUnbanUser("uid", false, null);
    }

    @Test @DisplayName("GET /api/v1/admin/organizations - get orgs")
    void getAllOrganizations() throws Exception {
        Page<com.ween.dto.response.OrganizationResponse> page = new PageImpl<>(List.of());
        when(adminService.getAllOrganizations(eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/organizations"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("PUT /api/v1/admin/organizations/{id}/verify - verify org")
    void verifyOrganization() throws Exception {
        when(adminService.verifyOrganization(eq("oid"), eq(true), eq(null)))
                .thenReturn(new OrganizationResponse());

        mockMvc.perform(put("/api/v1/admin/organizations/oid/verify")
                        .param("verify", "true"))
                .andExpect(status().isOk());

        verify(adminService).verifyOrganization("oid", true, null);
    }

    @Test @DisplayName("GET /api/v1/admin/stats - get stats")
    void getAdminStats() throws Exception {
        AdminStatsResponse stats = new AdminStatsResponse();
        when(adminService.getPlatformStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk());
    }
}
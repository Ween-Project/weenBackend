package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.BadgeRequest;
import com.ween.dto.response.AdminStatsResponse;
import com.ween.dto.response.BadgeResponse;
import com.ween.dto.response.OrganizationResponse;
import com.ween.dto.response.UserResponse;
import com.ween.enums.AchievementType;
import com.ween.enums.BadgeType;
import com.ween.service.AdminService;
import com.ween.service.BadgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AdminControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AdminService adminService;
    private BadgeService badgeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        badgeService = mock(BadgeService.class);
        mockMvc = standaloneSetup(new AdminController(adminService, badgeService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getAllUsersReturnsPage() throws Exception {
        UserResponse user = UserResponse.builder().id("user-1").username("ali").build();
        when(adminService.getAllUsers(any(), any()))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/api/v1/admin/users").param("search", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].username").value("ali"));
    }

    @Test
    void banUserDelegatesToService() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/user-1/ban-user")
                        .param("reason", "spam"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User banned successfully"));

        verify(adminService).banUser("user-1", "spam");
    }

    @Test
    void unbanUserDelegatesToService() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/user-1/unban-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User unbanned successfully"));

        verify(adminService).unbanUser("user-1");
    }

    @Test
    void verifyOrganizationReturnsUpdatedOrganization() throws Exception {
        when(adminService.verifyOrganization("org-1", true, "ok"))
                .thenReturn(OrganizationResponse.builder().id("org-1").isVerified(true).build());

        mockMvc.perform(put("/api/v1/admin/organizations/org-1/verify")
                        .param("verify", "true")
                        .param("note", "ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isVerified").value(true));
    }

    @Test
    void getPlatformStatisticsReturnsStats() throws Exception {
        when(adminService.getPlatformStatistics()).thenReturn(AdminStatsResponse.builder().totalUsers(7L).build());

        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(7));
    }

    @Test
    void badgeEndpointsDelegateToBadgeService() throws Exception {
        BadgeResponse badge = BadgeResponse.builder().id("badge-1").name("Starter").build();
        BadgeRequest request = new BadgeRequest();
        request.setName("Starter");
        request.setType(BadgeType.BRONZE);
        request.setAchievementType(AchievementType.PROFILE_COMPLETION);
        request.setAchievementThreshold(1);
        when(badgeService.create(any(BadgeRequest.class))).thenReturn(badge);
        when(badgeService.list(any())).thenReturn(new PageImpl<>(List.of(badge), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Starter"));

        mockMvc.perform(post("/api/v1/admin/badges")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Badge created successfully"));

        mockMvc.perform(delete("/api/v1/admin/badges/badge-1"))
                .andExpect(status().isOk());
        verify(badgeService).deactivate("badge-1");
    }
}

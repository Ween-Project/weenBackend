package com.ween.controller;

import com.ween.security.SecurityUtil;
import com.ween.service.FollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class FollowControllerTest {

    private FollowService followService;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        followService = mock(FollowService.class);
        securityUtil = mock(SecurityUtil.class);
        mockMvc = standaloneSetup(new FollowController(followService, securityUtil)).build();
    }

    @Test
    void followAndUnfollowUseCurrentUser() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("me");

        mockMvc.perform(post("/api/v1/users/target/follow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully followed user"));
        mockMvc.perform(delete("/api/v1/users/target/follow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully unfollowed user"));

        verify(followService).followUser("me", "target");
        verify(followService).unfollowUser("me", "target");
    }

    @Test
    void followersEndpointReturnsPage() throws Exception {
        when(followService.getFollowers("target", 0, 10))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/v1/users/target/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Followers retrieved successfully"));
    }
}

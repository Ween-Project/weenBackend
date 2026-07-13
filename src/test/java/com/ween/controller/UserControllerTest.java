package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ween.dto.request.UpdateProfileRequest;
import com.ween.dto.response.PublicProfileResponse;
import com.ween.entity.User;
import com.ween.mapper.CertificateMapper;
import com.ween.mapper.UserMapper;
import com.ween.service.BadgeService;
import com.ween.service.CertificateService;
import com.ween.service.FollowService;
import com.ween.service.RegistrationService;
import com.ween.service.UserService;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UserService userService;
    private RegistrationService registrationService;
    private CertificateService certificateService;
    private FollowService followService;
    private BadgeService badgeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        registrationService = mock(RegistrationService.class);
        certificateService = mock(CertificateService.class);
        followService = mock(FollowService.class);
        badgeService = mock(BadgeService.class);
        mockMvc = standaloneSetup(new UserController(
                userService,
                registrationService,
                certificateService,
                followService,
                mock(UserMapper.class),
                mock(CertificateMapper.class),
                badgeService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        ControllerTestSupport.authenticateAs("user-1");
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void getAndUpdateCurrentProfileUseAuthenticatedUser() throws Exception {
        User user = User.builder().username("ali").fullName("Ali").build();
        user.setId("user-1");
        when(userService.getUserById("user-1")).thenReturn(user);
        when(userService.updateProfile(any(), any())).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ali"));
        mockMvc.perform(multipart("/api/v1/users/me")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "request", "", "application/json", objectMapper.writeValueAsBytes(new UpdateProfileRequest())))
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));
    }

    @Test
    void publicProfileAndSearchReturnResponses() throws Exception {
        when(userService.getPublicProfile("ali", "user-1"))
                .thenReturn(PublicProfileResponse.builder().username("ali").build());
        when(userService.searchPublicProfiles(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(PublicProfileResponse.builder().username("ali").build()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users/@ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ali"));
        mockMvc.perform(get("/api/v1/users/search").param("query", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("ali"));
    }

    @Test
    void userRelatedCollectionsDelegateToServices() throws Exception {
        when(registrationService.getUserEvents(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(certificateService.getUserCertificatesPage(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        when(badgeService.getUserBadges(any())).thenReturn(List.of());
        when(userService.getUserCoinBalance("user-1")).thenReturn(99);
        when(followService.getFollowers("user-1", 0, 10)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(followService.getFollowing("user-1", 0, 10)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/v1/users/me/events")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me/certificates")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me/badges")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me/coins")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(99));
        mockMvc.perform(get("/api/v1/users/me/followers")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me/following")).andExpect(status().isOk());

        verify(badgeService).getUserBadges("user-1");
    }
}

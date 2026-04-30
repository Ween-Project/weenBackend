package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.UpdateProfileRequest;
import com.ween.dto.response.EventResponse;
import com.ween.dto.response.PublicProfileResponse;
import com.ween.entity.Certificate;
import com.ween.entity.User;
import com.ween.mapper.CertificateMapper;
import com.ween.mapper.UserMapper;
import com.ween.security.JwtUtil;
import com.ween.service.CertificateService;
import com.ween.service.RegistrationService;
import com.ween.service.UserService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserService userService;
    @MockBean private RegistrationService registrationService;
    @MockBean private CertificateService certificateService;
    @MockBean private UserMapper userMapper;
    @MockBean private CertificateMapper certificateMapper;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user-id", null, List.of())
        );
    }

    @Test @DisplayName("GET /api/v1/users/me - get profile")
    void getCurrentUserProfile() throws Exception {
        User res = User.builder().username("testuser").build();
        when(userService.getUserById("test-user-id")).thenReturn(res);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test @DisplayName("PUT /api/v1/users/me - update profile")
    void updateProfile() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Updated");
        User res = User.builder().fullName("Updated").build();
        
        when(userService.updateProfile(eq("test-user-id"), any())).thenReturn(res);

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated"));
    }

    @Test @DisplayName("GET /api/v1/users/@{username} - public profile")
    void getPublicProfile() throws Exception {
        PublicProfileResponse res = new PublicProfileResponse();
        when(userService.getPublicProfile("testuser")).thenReturn(res);

        mockMvc.perform(get("/api/v1/users/@testuser"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("GET /api/v1/users/me/events - user events")
    void getUserEvents() throws Exception {
        Page<EventResponse> page = new PageImpl<>(List.of());
        when(registrationService.getUserEvents(eq("test-user-id"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/users/me/events"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("GET /api/v1/users/me/certificates - user certificates")
    void getUserCertificates() throws Exception {
        when(certificateService.getUserCertificates("test-user-id")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users/me/certificates"))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("GET /api/v1/users/me/coins - user coins")
    void getUserCoins() throws Exception {
        when(userService.getUserCoinBalance("test-user-id")).thenReturn(500);

        mockMvc.perform(get("/api/v1/users/me/coins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(500));
    }
}

package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.*;
import com.ween.dto.response.AuthResponse;
import com.ween.dto.response.OrganizationResponse;
import com.ween.dto.response.UserResponse;
import com.ween.security.JwtUtil;
import com.ween.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;

    @Test @DisplayName("POST /api/v1/auth/register - 201 Created")
    void register() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("user"); req.setEmail("u@e.com"); req.setPassword("pass1234"); req.setFullName("Name");

        AuthResponse res = AuthResponse.builder().accessToken("token").user(UserResponse.builder().email("u@e.com").build()).build();
        when(authService.register(any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token"));
    }

    @Test @DisplayName("POST /api/v1/auth/register/organization - 201 Created")
    void registerOrganization() throws Exception {
        RegisterOrganizationRequest req = new RegisterOrganizationRequest();
        req.setUsername("org"); req.setEmail("o@e.com"); req.setPassword("pass1234");
        req.setOrganizationName("Org"); req.setDescription("Desc");

        AuthResponse res = AuthResponse.builder().accessToken("token").organization(OrganizationResponse.builder().email("o@e.com").build()).build();
        when(authService.registerOrganization(any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/auth/register/organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("token"));
    }

    @Test @DisplayName("POST /api/v1/auth/login - 200 OK")
    void login() throws Exception {
        LoginRequest req = new LoginRequest("u@e.com", "pass");
        AuthResponse res = AuthResponse.builder().accessToken("token").user(UserResponse.builder().email("u@e.com").build()).build();
        when(authService.login(any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("token"));
    }

    @Test @DisplayName("POST /api/v1/auth/login/organization - 200 OK")
    void loginOrganization() throws Exception {
        LoginRequest req = new LoginRequest("o@e.com", "pass");
        AuthResponse res = AuthResponse.builder().accessToken("token").organization(OrganizationResponse.builder().email("o@e.com").build()).build();
        when(authService.loginOrganization(any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/auth/login/organization")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("token"));
    }

    @Test @DisplayName("POST /api/v1/auth/refresh - 200 OK")
    void refresh() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest("rt");
        when(authService.refreshToken("rt")).thenReturn("new-at");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("new-at"));
    }

    @Test @DisplayName("POST /api/v1/auth/logout - 200 OK")
    void logout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk());
        verify(authService).logout();
    }

    @Test @DisplayName("GET /api/v1/auth/verify-token - 200 OK")
    void generateVerifyToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/verify-token"))
                .andExpect(status().isOk());
        verify(authService).sendVerificationTokenForCurrentUser();
    }

    @Test @DisplayName("POST /api/v1/auth/verify-token - 200 OK")
    void verifyToken() throws Exception {
        VerifyEmailRequest req = new VerifyEmailRequest("tok");
        mockMvc.perform(post("/api/v1/auth/verify-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(authService).verifyEmail("tok");
    }

    @Test @DisplayName("POST /api/v1/auth/forgot-password - 200 OK")
    void forgotPassword() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("u@e.com");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(authService).sendPasswordResetLink("u@e.com");
    }

    @Test @DisplayName("POST /api/v1/auth/reset-password - 200 OK")
    void resetPassword() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest("tok", "newpass123");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(authService).resetPasswordWithToken(any());
    }

    @Test @DisplayName("POST /api/v1/auth/change-password - 200 OK")
    void changePassword() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest("old", "newpass123");
        mockMvc.perform(post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(authService).changePasswordForCurrentUser(any());
    }
}

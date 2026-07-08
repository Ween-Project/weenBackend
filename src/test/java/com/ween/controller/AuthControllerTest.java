package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.LoginRequest;
import com.ween.dto.request.RefreshTokenRequest;
import com.ween.dto.response.AuthResponse;
import com.ween.dto.response.UserResponse;
import com.ween.enums.UserRole;
import com.ween.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void loginReturnsTokensInBodyAndCookies() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(900)
                .user(UserResponse.builder()
                        .id("user-1")
                        .email("ali@example.com")
                        .username("ali")
                        .role(UserRole.VOLUNTEER)
                        .build())
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ali@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(cookie().value("accessToken", "access-token"))
                .andExpect(cookie().value("refreshToken", "refresh-token"));
    }

    @Test
    void refreshUsesCookieTokenBeforeRequestBodyToken() throws Exception {
        when(authService.refreshToken("cookie-refresh")).thenReturn("new-access");
        RefreshTokenRequest body = new RefreshTokenRequest();
        body.setRefreshToken("body-refresh");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "cookie-refresh"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("new-access"))
                .andExpect(header().string("Set-Cookie", containsString("accessToken=new-access")));

        verify(authService).refreshToken("cookie-refresh");
    }

    @Test
    void loginRejectsInvalidPayloadBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}

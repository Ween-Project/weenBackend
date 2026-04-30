package com.ween.controller;

import com.ween.entity.Notification;
import com.ween.security.JwtUtil;
import com.ween.service.NotificationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private NotificationService notificationService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user-id", null, List.of())
        );
    }

    @Test @DisplayName("GET /api/v1/notifications - get notifications")
    void getNotifications() throws Exception {
        Page<com.ween.dto.response.NotificationResponse> page = new PageImpl<>(List.of(com.ween.dto.response.NotificationResponse.builder().title("Title").build()));
        when(notificationService.getUserNotificationsMapped(eq("user-id"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Title"));
    }

    @Test @DisplayName("PUT /api/v1/notifications/{id}/read - mark as read")
    void markAsRead() throws Exception {
        Notification n = Notification.builder().isRead(true).build();
        when(notificationService.markAsRead("user-id", "nid")).thenReturn(n);

        mockMvc.perform(put("/api/v1/notifications/nid/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test @DisplayName("PUT /api/v1/notifications/read-all - mark all as read")
    void markAllAsRead() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isOk());
        verify(notificationService).markAllAsRead("user-id");
    }
}

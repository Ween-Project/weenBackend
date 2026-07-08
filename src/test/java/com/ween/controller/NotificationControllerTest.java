package com.ween.controller;

import com.ween.dto.response.NotificationResponse;
import com.ween.entity.Notification;
import com.ween.mapper.NotificationMapper;
import com.ween.service.NotificationService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class NotificationControllerTest {

    private NotificationService notificationService;
    private NotificationMapper notificationMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        notificationMapper = mock(NotificationMapper.class);
        mockMvc = standaloneSetup(new NotificationController(notificationService, notificationMapper))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        ControllerTestSupport.authenticateAs("user-1");
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void getNotificationsMapsPage() throws Exception {
        Notification notification = Notification.builder().userId("user-1").title("Hi").build();
        when(notificationService.getUserNotifications(any(), any()))
                .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1));
        when(notificationMapper.toNotificationResponse(notification))
                .thenReturn(NotificationResponse.builder().title("Hi").build());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Hi"));
    }

    @Test
    void markReadEndpointsUseCurrentUser() throws Exception {
        when(notificationService.markAsRead("user-1", "n-1")).thenReturn(Notification.builder().userId("user-1").build());

        mockMvc.perform(put("/api/v1/notifications/n-1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification marked as read"));
        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));

        verify(notificationService).markAllAsRead("user-1");
    }
}

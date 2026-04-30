package com.ween.service;

import com.ween.entity.Notification;
import com.ween.enums.NotificationType;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.NotificationMapper;
import com.ween.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;
    @InjectMocks private NotificationService notificationService;

    @Test @DisplayName("Create notification – success")
    void createNotification() {
        when(notificationRepository.save(any())).thenAnswer(i -> { Notification n = i.getArgument(0); n.setId("nid"); return n; });
        Notification n = notificationService.createNotification("uid", NotificationType.SYSTEM, "Title", "Body");
        assertThat(n.getUserId()).isEqualTo("uid");
        assertThat(n.getType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(n.getIsRead()).isFalse();
    }

    @Test @DisplayName("Create registration notification")
    void createRegistrationNotification() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Notification n = notificationService.createRegistrationNotification("uid", "Test Event");
        assertThat(n.getTitle()).isEqualTo("Registration Successful");
        assertThat(n.getBody()).contains("Test Event");
    }

    @Test @DisplayName("Create certificate notification")
    void createCertificateNotification() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Notification n = notificationService.createCertificateNotification("uid", "Ev", "CERT-1");
        assertThat(n.getTitle()).isEqualTo("Certificate Ready");
        assertThat(n.getBody()).contains("CERT-1");
    }

    @Test @DisplayName("Get notification by id – found")
    void getById_found() {
        Notification n = Notification.builder().userId("uid").title("T").build(); n.setId("nid");
        when(notificationRepository.findById("nid")).thenReturn(Optional.of(n));
        assertThat(notificationService.getNotificationById("nid")).isEqualTo(n);
    }

    @Test @DisplayName("Get notification by id – not found throws")
    void getById_notFound() {
        when(notificationRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.getNotificationById("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Mark as read – success")
    void markAsRead_success() {
        Notification n = Notification.builder().userId("uid").isRead(false).build(); n.setId("nid");
        when(notificationRepository.findById("nid")).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.markAsRead("uid", "nid");
        assertThat(result.getIsRead()).isTrue();
    }

    @Test @DisplayName("Mark as read – wrong user throws")
    void markAsRead_wrongUser() {
        Notification n = Notification.builder().userId("uid").isRead(false).build(); n.setId("nid");
        when(notificationRepository.findById("nid")).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> notificationService.markAsRead("other", "nid")).isInstanceOf(AccessDeniedException.class);
    }

    @Test @DisplayName("Mark all as read")
    void markAllAsRead() {
        notificationService.markAllAsRead("uid");
        verify(notificationRepository).markAllReadByUserId("uid");
    }

    @Test @DisplayName("Get user notifications")
    void getUserNotifications() {
        Pageable pageable = PageRequest.of(0, 10);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("uid", pageable))
                .thenReturn(Page.empty(pageable));
        Page<Notification> result = notificationService.getUserNotifications("uid", pageable);
        assertThat(result.getContent()).isEmpty();
    }
}

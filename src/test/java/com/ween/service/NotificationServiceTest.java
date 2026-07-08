package com.ween.service;

import com.ween.entity.Notification;
import com.ween.enums.NotificationType;
import com.ween.mapper.NotificationMapper;
import com.ween.repository.EventRepository;
import com.ween.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationMapper notificationMapper;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock EventRepository eventRepository;
    @InjectMocks NotificationService notificationService;

    @Test
    void createNotificationSavesAndPushesNotification() {
        Notification saved = Notification.builder().userId("user-1").type(NotificationType.SYSTEM).title("Hi").build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        Notification result = notificationService.createNotification("user-1", NotificationType.SYSTEM, "Hi", "Body");

        assertThat(result).isSameAs(saved);
        verify(messagingTemplate).convertAndSendToUser("user-1", "/queue/notifications", notificationMapper.toNotificationResponse(saved));
    }
}

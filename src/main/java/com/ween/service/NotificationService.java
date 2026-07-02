package com.ween.service;

import com.ween.entity.Event;
import com.ween.entity.Notification;
import com.ween.enums.NotificationType;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.NotificationMapper;
import com.ween.repository.EventRepository;
import com.ween.repository.NotificationRepository;
import com.ween.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EventRepository eventRepository;

    private String getEventTitle(String eventId) {
        return eventRepository.findById(eventId).map(Event::getTitle).orElse("Unknown Event");
    }

    public Notification createNotification(String userId, NotificationType type, String title, String body) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created for user: {} with type: {}", userId, type);

        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notificationMapper.toNotificationResponse(saved));

        return saved;
    }

    public Notification createRegistrationNotification(String userId, String eventId) {
        String eventTitle = getEventTitle(eventId);
        String title = "Registration Successful";
        String body = "You have registered for " + eventTitle;
        return createNotification(userId, NotificationType.SYSTEM, title, body);
    }

    public Notification createMessageNotification(String userId, String senderName, String content) {
        String title = "New message : " + senderName;
        String body = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        return createNotification(userId, NotificationType.NEW_MESSAGE, title, body);
    }

    public Notification createCertificateNotification(String userId, String eventId, String certificateNumber) {
        String eventTitle = getEventTitle(eventId);
        String title = "Certificate Ready";
        String body = "Your certificate for " + eventTitle + " is ready. Number: " + certificateNumber;
        return createNotification(userId, NotificationType.CERTIFICATE_READY, title, body);
    }

    public Notification createEventReminderNotification(String userId, String eventId, String timeStr) {
        String eventTitle = getEventTitle(eventId);
        String title = "Event Reminder";
        String body = "The event " + eventTitle + " will start at " + timeStr + ". Don't be late!";
        return createNotification(userId, NotificationType.EVENT_REMINDER, title, body);
    }

    public Notification createAttendanceConfirmedNotification(String userId, String eventId) {
        String eventTitle = getEventTitle(eventId);
        String title = "Attendance Confirmed";
        String body = "Your attendance for " + eventTitle + " has been confirmed.";
        return createNotification(userId, NotificationType.ATTENDANCE_CONFIRMED, title, body);
    }

    public Notification createCoinEarnedNotification(String userId, Integer amount, String reason) {
        String title = "Coins Earned!";
        String body = "You have earned " + amount + " coins for: " + reason;
        return createNotification(userId, NotificationType.COIN_EARNED, title, body);
    }


    public Notification getNotificationById(String notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
    }

    public Page<Notification> getUserNotifications(String userId, Pageable pageable) {
        log.info("Fetching notifications for user: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<NotificationResponse> getUserNotificationsMapped(String userId, Pageable pageable) {
        log.info("Fetching notifications (mapped) for user: {}", userId);
        Page<Notification> notifications = getUserNotifications(userId, pageable);

        var mappedNotifications = notifications.getContent().stream()
                .map(notificationMapper::toNotificationResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(mappedNotifications, pageable, notifications.getTotalElements());
    }

    public Notification markAsRead(String userId, String notificationId) {
        Notification notification = getNotificationById(notificationId);

        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("Notification does not belong to user");
        }

        notification.setIsRead(true);
        Notification updated = notificationRepository.save(notification);
        log.info("Notification marked as read: {}", notificationId);
        return updated;
    }

    public void markAllAsRead(String userId) {
        notificationRepository.markAllReadByUserId(userId);
        log.info("All notifications marked as read for user: {}", userId);
    }

}
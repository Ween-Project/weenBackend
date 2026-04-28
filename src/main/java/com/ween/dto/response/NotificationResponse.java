package com.ween.dto.response;

import com.ween.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private String id;
    private NotificationType type;
    private String title;
    private String body;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
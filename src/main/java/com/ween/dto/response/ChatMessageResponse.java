package com.ween.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String senderId;
    private String recipientId;

    private String content;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private Boolean request;
}


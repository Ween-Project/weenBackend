package com.ween.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatConversationResponse {
    private String partnerId;
    private String lastMessageId;
    private String lastMessage;
    private String lastSenderId;
    private boolean lastMessageRead;
    private long unreadCount;
    private LocalDateTime lastMessageAt;
}
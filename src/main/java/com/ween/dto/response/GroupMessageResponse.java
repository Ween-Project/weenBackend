package com.ween.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GroupMessageResponse {
    private String id;
    private String senderId;
    private String chatRoomId;
    private String content;
    private LocalDateTime createdAt;
    
    // Sender details for UI
    private String senderUsername;
    private String senderFullName;
    private String senderPhotoUrl;

    // Reply functionality
    private String replyToMessageId;
    private String replyToMessageContent;
}

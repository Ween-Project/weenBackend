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
}

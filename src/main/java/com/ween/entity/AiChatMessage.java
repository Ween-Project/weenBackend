package com.ween.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_chat_messages", indexes = {
        @Index(name = "idx_ai_chat_user", columnList = "user_id"),
        @Index(name = "idx_ai_chat_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatMessage extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 10)
    private String sender; // "USER" or "AI"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

}

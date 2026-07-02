package com.ween.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_room", columnList = "chat_room_id"),
        @Index(name = "idx_chat_sender", columnList = "sender_id"),
        @Index(name = "idx_chat_recipient", columnList = "recipient_id"),
        @Index(name = "idx_chat_conversation_created", columnList = "sender_id, recipient_id, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @Column(name = "chat_room_id", length = 36)
    private String chatRoomId;

    @Column(name = "sender_id", nullable = false, length = 36)
    private String senderId;

    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "read_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime readAt;
}
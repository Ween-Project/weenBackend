package com.ween.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "group_chat_messages", indexes = {
        @Index(name = "idx_group_chat_room", columnList = "chat_room_id"),
        @Index(name = "idx_group_chat_sender", columnList = "sender_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupChatMessage extends BaseEntity {

    @Column(name = "chat_room_id", nullable = false, length = 36)
    private String chatRoomId;

    @Column(name = "sender_id", nullable = false, length = 36)
    private String senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

}

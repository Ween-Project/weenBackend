package com.ween.entity;

import com.ween.enums.ChatRoomRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chat_room_members", indexes = {
        @Index(name = "idx_room_member_room", columnList = "chat_room_id"),
        @Index(name = "idx_room_member_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomMember extends BaseEntity {

    @Column(name = "chat_room_id", nullable = false, length = 36)
    private String chatRoomId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomRole role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
}

package com.ween.entity;

import com.ween.enums.ChatRoomType;
import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chat_rooms", indexes = {
        @Index(name = "idx_room_type", columnList = "type"),
        @Index(name = "idx_chatroom_event_id", columnList = "event_id"),
        @Index(name = "idx_participants", columnList = "participant_one_id, participant_two_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {

    @Column(length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomType type;

    @Column(name = "event_id", columnDefinition = "CHAR(36)")
    private String eventId;

    @Column(name = "participant_one_id", columnDefinition = "CHAR(36)")
    private String participantOneId;

    @Column(name = "participant_two_id", columnDefinition = "CHAR(36)")
    private String participantTwoId;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "creator_id", columnDefinition = "CHAR(36)")
    private String creatorId;
}


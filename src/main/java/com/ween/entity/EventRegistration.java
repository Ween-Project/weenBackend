package com.ween.entity;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "event_registrations", indexes = {
    @Index(name = "idx_event_id", columnList = "event_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "uq_event_user", columnList = "event_id,user_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration extends BaseEntity {

    @Column(name = "event_id", columnDefinition = "CHAR(36)", nullable = false)
    private String eventId;

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Column(name = "registered_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime registeredAt;

    @Column(name = "custom_answers", columnDefinition = "JSON")
    private String customAnswers;

    @Column(name = "is_joined")
    @Builder.Default
    private Boolean isJoined = false;

    @Column(name = "joined_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime joinedAt;
}

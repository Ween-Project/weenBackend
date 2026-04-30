package com.ween.entity;

import com.ween.enums.ParticipationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    private ParticipationStatus status;

    private LocalDateTime joinedAt;
}

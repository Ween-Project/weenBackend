package com.ween.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard_entries", indexes = {
    @Index(name = "idx_lb_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "coin_count")
    private Integer coinCount;

    @Column(name = "calculated_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime calculatedAt;
}

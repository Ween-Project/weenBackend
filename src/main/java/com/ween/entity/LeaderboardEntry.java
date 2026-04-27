package com.ween.entity;

import com.ween.enums.LeaderboardPeriod;
import com.ween.enums.LeaderboardScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard_entries", indexes = {
    @Index(name = "idx_lb_user_id", columnList = "user_id"),
    @Index(name = "idx_lb_period_scope", columnList = "period,scope")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaderboardPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaderboardScope scope;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "coin_count")
    private Integer coinCount;

    @Column(name = "calculated_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime calculatedAt;
}

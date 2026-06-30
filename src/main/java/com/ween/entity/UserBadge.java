package com.ween.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_badges", indexes = {
        @Index(name = "idx_ub_user_id", columnList = "user_id"),
        @Index(name = "idx_ub_badge_id", columnList = "badge_id"),
        @Index(name = "idx_ub_special_key", columnList = "special_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBadge extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt;

    // Optional field to distinguish context, e.g., "MONTHLY_WINNER_2026_06" or "EVENT_123"
    @Column(name = "special_key", length = 100)
    private String specialKey;
}

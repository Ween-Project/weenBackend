package com.ween.entity;

import com.ween.enums.BadgeType;
import com.ween.enums.AchievementType;
import com.ween.enums.EventCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "badges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BadgeType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_type", length = 50)
    private AchievementType achievementType;

    @Column(name = "achievement_threshold", nullable = false)
    private Integer achievementThreshold;

    // Required only for EVENT_CATEGORY_ATTENDANCE_COUNT.
    @Enumerated(EnumType.STRING)
    @Column(name = "event_category", length = 50)
    private EventCategory eventCategory;

    @Column(nullable = false)
    private Integer points = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active")
    private Boolean isActive = true;
}

package com.ween.dto.response;

import com.ween.enums.AchievementType;
import com.ween.enums.BadgeType;
import com.ween.enums.EventCategory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BadgeResponse {
    private String id;
    private String name;
    private String description;
    private BadgeType type;
    private AchievementType achievementType;
    private Integer achievementThreshold;
    private EventCategory eventCategory;
    private Integer points;
    private String imageUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
}

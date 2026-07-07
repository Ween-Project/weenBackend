package com.ween.dto.request;

import com.ween.enums.AchievementType;
import com.ween.enums.BadgeType;
import com.ween.enums.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BadgeRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private BadgeType type;
    @NotNull
    private AchievementType achievementType;
    @NotNull
    @Min(1)
    private Integer achievementThreshold;
    private EventCategory eventCategory;
    @Min(0)
    private Integer points = 0;
    private String imageUrl;
    private Boolean isActive = true;
}

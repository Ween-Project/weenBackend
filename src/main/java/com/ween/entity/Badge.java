package com.ween.entity;

import com.ween.enums.BadgeType;
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

    // Optional: Only used if type == EVENT_CATEGORY
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

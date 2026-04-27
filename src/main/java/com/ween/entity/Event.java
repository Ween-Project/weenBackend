package com.ween.entity;

import com.ween.enums.EventCategory;
import com.ween.enums.EventStatus;
import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_organization_id", columnList = "organization_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_start_date", columnList = "start_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    @Column(length = 300, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCategory category;

    @Column(length = 100)
    private String city;

    @Column(length = 300)
    private String address;

    @Column(name = "is_online")
    private Boolean isOnline = false;

    @Column(name = "start_date", columnDefinition = "DATETIME(6)")
    private LocalDateTime startDate;

    @Column(name = "end_date", columnDefinition = "DATETIME(6)")
    private LocalDateTime endDate;

    @Column(name = "registration_deadline", columnDefinition = "DATETIME(6)")
    private LocalDateTime registrationDeadline;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "organization_id", columnDefinition = "CHAR(36)", nullable = false)
    private String organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields;
}

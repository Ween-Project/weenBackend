package com.ween.dto.response;

import com.ween.enums.EventCategory;
import com.ween.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailResponse {
    private String id;
    private String title;
    private String description;
    private EventCategory category;
    private String city;
    private String address;
    private Boolean isOnline;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime registrationDeadline;
    private Integer maxParticipants;
    private Integer currentRegistrations;
    private Integer attendeeCount;
    private String organizationId;
    private String organizationName;
    private EventStatus status;
    private String coverImageUrl;
    private String customFields;
    private Boolean userRegistered;
    private Boolean userAttended;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

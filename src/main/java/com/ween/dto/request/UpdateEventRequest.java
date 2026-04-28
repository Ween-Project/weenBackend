package com.ween.dto.request;

import com.ween.enums.EventCategory;
import com.ween.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {
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
    private EventStatus status;
    private String coverImageUrl;
    private String customFields;
}
package com.ween.dto.request;

import com.ween.enums.EventCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventRequest {

    @NotBlank(message = "Event title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @NotBlank(message = "Event description is required")
    private String description;

    @NotNull(message = "Category is required")
    private EventCategory category;

    private String city;
    private String address;

    @NotNull(message = "Online status is required")
    private Boolean isOnline;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private LocalDateTime registrationDeadline;

    @Min(value = 1, message = "Max participants must be at least 1")
    private Integer maxParticipants;

    private String coverImageUrl;

    private String customFields;
}

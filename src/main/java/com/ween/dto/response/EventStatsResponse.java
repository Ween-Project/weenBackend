package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStatsResponse {
    private String eventId;
    private String eventTitle;
    private Long totalRegistered;
    private Long totalAttended;
    private Long registrationRate;
    private Long attendanceRate;
}

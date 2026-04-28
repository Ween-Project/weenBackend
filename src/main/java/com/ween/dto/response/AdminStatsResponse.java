package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsResponse {
    private Long totalUsers;
    private Long totalOrganizations;
    private Long totalEvents;
    private Long totalRegistrations;
    private Long totalAttendees;
    private Long totalCoinsDistributed;
    private Long totalCertificatesIssued;
}

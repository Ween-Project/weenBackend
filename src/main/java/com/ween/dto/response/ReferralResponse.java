package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralResponse {
    private String id;
    private String referrerId;
    private String referrerName;
    private String referredId;
    private String referredName;
    private Boolean coinAwarded;
    private LocalDateTime createdAt;
}

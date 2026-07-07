package com.ween.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserBadgeResponse {
    private String id;
    private BadgeResponse badge;
    private LocalDateTime earnedAt;
}

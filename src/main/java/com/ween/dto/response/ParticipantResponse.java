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
public class ParticipantResponse {
    private String id;
    private String username;
    private String fullName;
    private String profilePhotoUrl;
    private Integer weenCoinBalance;
    private LocalDateTime registeredAt;
    private LocalDateTime joinedAt;
    private Boolean isJoined;
}
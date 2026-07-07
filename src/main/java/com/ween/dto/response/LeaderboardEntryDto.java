package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDto {
    private Integer rank;
    private String userId;
    private String username;
    private String fullName;
    private String profilePhotoUrl;
    private Integer coins;
    private String university;
    private String major;
    private String course;
    private String skills;
    private String interests;

    public LeaderboardEntryDto(Integer rank, String username, String profilePhotoUrl, Integer coins) {
        this.rank = rank;
        this.username = username;
        this.profilePhotoUrl = profilePhotoUrl;
        this.coins = coins;
    }
}

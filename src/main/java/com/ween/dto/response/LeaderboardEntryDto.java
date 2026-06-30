package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDto {
    private Integer rank;
    private String username;
    private String profilePhotoUrl;
    private Integer coins;
}


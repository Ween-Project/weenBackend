package com.ween.dto.response;

import com.ween.enums.LeaderboardPeriod;
import com.ween.enums.LeaderboardScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryResponse {
    private String userId;
    private String username;
    private String profilePhotoUrl;
    private Integer rankPosition;
    private Integer coinCount;
    private LeaderboardPeriod period;
    private LeaderboardScope scope;
}

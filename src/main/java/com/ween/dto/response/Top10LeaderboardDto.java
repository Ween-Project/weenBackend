package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Top10LeaderboardDto {
    private List<LeaderboardEntryDto> entries;
}

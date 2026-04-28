package com.ween.mapper;

import com.ween.dto.response.LeaderboardEntryResponse;
import com.ween.entity.LeaderboardEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaderboardEntryMapper {
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "profilePhotoUrl", ignore = true)
    LeaderboardEntryResponse toLeaderboardEntryResponse(LeaderboardEntry leaderboardEntry);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "calculatedAt", ignore = true)
    LeaderboardEntry toLeaderboardEntry(LeaderboardEntryResponse response);
}

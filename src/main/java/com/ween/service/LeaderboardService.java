package com.ween.service;

import com.ween.dto.response.LeaderboardEntryDto;
import com.ween.dto.response.Top10LeaderboardDto;
import com.ween.entity.CoinTransaction;
import com.ween.entity.LeaderboardEntry;
import com.ween.entity.User;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.LeaderboardEntryRepository;
import com.ween.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;

    public Page<LeaderboardEntry> getActiveLeaderboard(Pageable pageable) {
        log.info("Fetching active monthly leaderboard");
        return leaderboardEntryRepository.findAllByOrderByRankPositionAsc(pageable);
    }

    public LeaderboardEntry getUserActiveLeaderboardPosition(String userId) {
        Page<LeaderboardEntry> entries = getActiveLeaderboard(Pageable.unpaged());
        return entries.getContent().stream()
                .filter(entry -> entry.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User not found in active leaderboard"));
    }

    public Integer getUserActiveRank(String userId) {
        try {
            return getUserActiveLeaderboardPosition(userId).getRankPosition();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    public List<LeaderboardEntry> getTopUsersActive(Integer limit) {
        return getActiveLeaderboard(PageRequest.of(0, limit)).getContent();
    }

    public Top10LeaderboardDto getTop10ActiveLeaderboard() {
        List<LeaderboardEntryDto> entryDtos = getTopUsersActive(10).stream()
                .map(entry -> {
                    User user = userRepository.findById(entry.getUserId()).orElse(null);
                    return new LeaderboardEntryDto(
                            entry.getRankPosition(),
                            user != null ? user.getUsername() : "Unknown",
                            user != null ? user.getProfilePhotoUrl() : null,
                            entry.getCoinCount()
                    );
                })
                .collect(Collectors.toList());

        return new Top10LeaderboardDto(entryDtos);
    }

    public Page<LeaderboardEntryDto> getLeaderboardMapped(String period, Pageable pageable) {
        if ("ALL_TIME".equalsIgnoreCase(period)) {
            return getAllTimeLeaderboard(pageable)
                    .map(user -> new LeaderboardEntryDto(
                            null, // rank is implicit by position
                            user.getUsername(),
                            user.getProfilePhotoUrl(),
                            user.getWeenCoinBalance()
                    ));
        } else {
            return getActiveLeaderboard(pageable)
                    .map(entry -> {
                        User user = userRepository.findById(entry.getUserId()).orElse(null);
                        return new LeaderboardEntryDto(
                                entry.getRankPosition(),
                                user != null ? user.getUsername() : "Unknown",
                                user != null ? user.getProfilePhotoUrl() : null,
                                entry.getCoinCount()
                        );
                    });
        }
    }

    public Page<User> getAllTimeLeaderboard(Pageable pageable) {
        return userRepository.findAllByOrderByWeenCoinBalanceDesc(pageable);
    }

    public Top10LeaderboardDto getTop10AllTimeLeaderboard() {
        List<LeaderboardEntryDto> entryDtos = getAllTimeLeaderboard(PageRequest.of(0, 10)).getContent().stream()
                .map(user -> new LeaderboardEntryDto(
                        null, // Rank can be calculated implicitly by the list order (1 to 10)
                        user.getUsername(),
                        user.getProfilePhotoUrl(),
                        user.getWeenCoinBalance()
                ))
                .collect(Collectors.toList());

        // Assign ranks correctly
        int rank = 1;
        for (LeaderboardEntryDto dto : entryDtos) {
            dto.setRank(rank++);
        }

        return new Top10LeaderboardDto(entryDtos);
    }



    private void saveLeaderboardEntries(Map<String, Integer> userScores) {
        List<Map.Entry<String, Integer>> sortedEntries = userScores.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            LeaderboardEntry leaderboardEntry = LeaderboardEntry.builder()
                    .userId(entry.getKey())
                    .rankPosition(rank)
                    .coinCount(entry.getValue())
                    .calculatedAt(LocalDateTime.now())
                    .build();

            leaderboardEntryRepository.save(leaderboardEntry);
            rank++;
        }
        log.info("Active Leaderboard recalculated with {} entries", sortedEntries.size());
    }


}

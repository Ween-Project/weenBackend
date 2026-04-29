package com.ween.service;

import com.ween.dto.response.LeaderboardEntryResponse;
import com.ween.entity.LeaderboardEntry;
import com.ween.entity.User;
import com.ween.enums.LeaderboardPeriod;
import com.ween.enums.LeaderboardScope;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.LeaderboardEntryMapper;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.LeaderboardEntryRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;
    private final CoinService coinService;
    private final LeaderboardEntryMapper leaderboardEntryMapper;

    public Page<LeaderboardEntry> getLeaderboard(LeaderboardPeriod period, LeaderboardScope scope, Pageable pageable) {
        log.info("Fetching leaderboard: period={}, scope={}", period, scope);
        return leaderboardEntryRepository.findByPeriodAndScopeOrderByRankPositionAsc(period, scope, pageable);
    }

    public Page<LeaderboardEntryResponse> getLeaderboardMapped(LeaderboardPeriod period, LeaderboardScope scope, Pageable pageable) {
        log.info("Fetching leaderboard (mapped): period={}, scope={}", period, scope);
        Page<LeaderboardEntry> entries = getLeaderboard(period, scope, pageable);

        List<LeaderboardEntryResponse> mappedEntries = entries.getContent().stream()
                .map(entry -> {
                    User user = userRepository.findById(entry.getUserId()).orElse(null);
                    LeaderboardEntryResponse response = leaderboardEntryMapper.toLeaderboardEntryResponse(entry);
                    if (user != null) {
                        response.setUsername(user.getUsername());
                        response.setProfilePhotoUrl(user.getProfilePhotoUrl());
                    }
                    return response;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(mappedEntries, pageable, entries.getTotalElements());
    }

    public LeaderboardEntry getUserLeaderboardPosition(String userId, LeaderboardPeriod period, LeaderboardScope scope) {
        Page<LeaderboardEntry> entries = getLeaderboard(period, scope, org.springframework.data.domain.Pageable.unpaged());
        return entries.getContent().stream()
                .filter(entry -> entry.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("User not found in leaderboard"));
    }

    public Integer getUserRank(String userId, LeaderboardPeriod period, LeaderboardScope scope) {
        try {
            return getUserLeaderboardPosition(userId, period, scope).getRankPosition();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    public Integer getUserTotalCoins(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getWeenCoinBalance();
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void recalculateMonthlyLeaderboard() {
        try {
            log.info("Starting monthly leaderboard recalculation");
            recalculateLeaderboard(LeaderboardPeriod.MONTHLY, LeaderboardScope.GLOBAL);
            log.info("Monthly leaderboard recalculation completed");
        } catch (Exception e) {
            log.error("Failed to recalculate monthly leaderboard", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void recalculateQuarterlyLeaderboard() {
        try {
            log.info("Starting quarterly leaderboard recalculation");
            recalculateLeaderboard(LeaderboardPeriod.QUARTERLY, LeaderboardScope.GLOBAL);
            log.info("Quarterly leaderboard recalculation completed");
        } catch (Exception e) {
            log.error("Failed to recalculate quarterly leaderboard", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void recalculateAnnualLeaderboard() {
        try {
            log.info("Starting annual leaderboard recalculation");
            recalculateLeaderboard(LeaderboardPeriod.ANNUAL, LeaderboardScope.GLOBAL);
            log.info("Annual leaderboard recalculation completed");
        } catch (Exception e) {
            log.error("Failed to recalculate annual leaderboard", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void recalculateAllTimeLeaderboard() {
        try {
            log.info("Starting all-time leaderboard recalculation");
            recalculateLeaderboard(LeaderboardPeriod.ALL_TIME, LeaderboardScope.GLOBAL);
            log.info("All-time leaderboard recalculation completed");
        } catch (Exception e) {
            log.error("Failed to recalculate all-time leaderboard", e);
        }
    }

    @Transactional
    public void recalculateLeaderboard(LeaderboardPeriod period, LeaderboardScope scope) {
        // Delete existing entries for this period and scope
        leaderboardEntryRepository.deleteByPeriodAndScope(period, scope);

        // Get all users
        List<User> users = userRepository.findAll();

        // Calculate scores for each user based on period
        Map<String, Integer> userScores = new HashMap<>();
        for (User user : users) {
            int score = calculateUserScoreForPeriod(user.getId(), period);
            userScores.put(user.getId(), score);
        }

        // Sort users by score descending
        List<Map.Entry<String, Integer>> sortedEntries = userScores.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .collect(Collectors.toList());

        // Create leaderboard entries
        int rank = 1;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            LeaderboardEntry leaderboardEntry = LeaderboardEntry.builder()
                    .userId(entry.getKey())
                    .period(period)
                    .scope(scope)
                    .rankPosition(rank)
                    .coinCount(entry.getValue())
                    .calculatedAt(LocalDateTime.now())
                    .build();

            leaderboardEntryRepository.save(leaderboardEntry);

            // Award leaderboard bonus to top performers
            if (rank <= 10) {
                try {
                    coinService.awardLeaderboardBonus(entry.getKey(), rank);
                } catch (Exception e) {
                    log.warn("Failed to award leaderboard bonus to user: {}", entry.getKey(), e);
                }
            }

            rank++;
        }

        log.info("Leaderboard recalculated with {} entries", sortedEntries.size());
    }
    private int calculateUserScoreForPeriod(String userId, LeaderboardPeriod period) {
        LocalDateTime startDate = calculatePeriodStartDate(period);
        LocalDateTime endDate = LocalDateTime.now();

        // Sum all coins earned in this period
        List<com.ween.entity.CoinTransaction> transactions = coinTransactionRepository.findByUserId(userId);
        return transactions.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(startDate) && t.getCreatedAt().isBefore(endDate))
                .map(com.ween.entity.CoinTransaction::getAmount)
                .reduce(0, Integer::sum);
    }

    private LocalDateTime calculatePeriodStartDate(LeaderboardPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case MONTHLY -> now.minusMonths(1);
            case QUARTERLY -> now.minusMonths(3);
            case ANNUAL -> now.minusYears(1);
            case ALL_TIME -> LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        };
    }

    public Top10LeaderboardDto getTop10Leaderboard(LeaderboardPeriod period, LeaderboardScope scope) {
        Page<LeaderboardEntry> entries = getLeaderboard(period, scope, org.springframework.data.domain.PageRequest.of(0, 10));
        
        List<LeaderboardEntryDto> entryDtos = entries.getContent().stream()
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

        return new Top10LeaderboardDto(period, scope, entryDtos);
    }

    public Integer getUserCoinsSinceDate(String userId, LocalDateTime sinceDate) {
        List<com.ween.entity.CoinTransaction> transactions = coinTransactionRepository.findByUserId(userId);
        return transactions.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(sinceDate))
                .map(com.ween.entity.CoinTransaction::getAmount)
                .reduce(0, Integer::sum);
    }

    public List<LeaderboardEntry> getTopUsers(LeaderboardPeriod period, LeaderboardScope scope, Integer limit) {
        Page<LeaderboardEntry> entries = getLeaderboard(period, scope, org.springframework.data.domain.PageRequest.of(0, limit));
        return entries.getContent();
    }

    // Inner DTOs for backward compatibility
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LeaderboardEntryDto {
        public Integer rank;
        public String username;
        public String profilePhotoUrl;
        public Integer coins;

    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class Top10LeaderboardDto {
        public LeaderboardPeriod period;
        public LeaderboardScope scope;
        public List<LeaderboardEntryDto> entries;

    }
}

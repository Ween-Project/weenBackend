package com.ween.service;

import com.ween.dto.response.LeaderboardEntryDto;
import com.ween.dto.response.Top10LeaderboardDto;
import com.ween.entity.Badge;
import com.ween.entity.CoinTransaction;
import com.ween.entity.LeaderboardEntry;
import com.ween.entity.User;
import com.ween.enums.BadgeType;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.BadgeRepository;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;
    private final BadgeRepository badgeRepository;

    /* =====================================================================================
     * MONTHLY (ACTIVE) LEADERBOARD FETCHING
     * ===================================================================================== */

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
                    return toLeaderboardDto(user, entry.getRankPosition(), entry.getCoinCount());
                })
                .collect(Collectors.toList());

        return new Top10LeaderboardDto(entryDtos);
    }

    public Page<LeaderboardEntryDto> getLeaderboardMapped(Pageable pageable) {
        Page<User> users = getAllTimeLeaderboard(pageable);
        AtomicInteger rank = new AtomicInteger((int) pageable.getOffset() + 1);
        List<LeaderboardEntryDto> entries = users.getContent().stream()
                .map(user -> toLeaderboardDto(user, rank.getAndIncrement(), user.getWeenCoinBalance()))
                .toList();
        return new PageImpl<>(entries, pageable, users.getTotalElements());
    }

    /* =====================================================================================
     * ALL-TIME LEADERBOARD FETCHING (Direct from User table)
     * ===================================================================================== */

    public Page<User> getAllTimeLeaderboard(Pageable pageable) {
        return userRepository.findAllByOrderByWeenCoinBalanceDesc(pageable);
    }

    public Top10LeaderboardDto getTop10AllTimeLeaderboard() {
        List<LeaderboardEntryDto> entryDtos = getAllTimeLeaderboard(PageRequest.of(0, 10)).getContent().stream()
                .map(user -> toLeaderboardDto(user, null, user.getWeenCoinBalance()))
                .collect(Collectors.toList());

        // Assign ranks correctly
        int rank = 1;
        for (LeaderboardEntryDto dto : entryDtos) {
            dto.setRank(rank++);
        }

        return new Top10LeaderboardDto(entryDtos);
    }

    private LeaderboardEntryDto toLeaderboardDto(User user, Integer rank, Integer coins) {
        if (user == null) {
            return LeaderboardEntryDto.builder()
                    .rank(rank)
                    .username("Unknown")
                    .coins(coins)
                    .build();
        }
        return LeaderboardEntryDto.builder()
                .rank(rank)
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .coins(coins)
                .university(user.getUniversity())
                .major(user.getMajor())
                .course(user.getCourse())
                .skills(user.getSkills())
                .interests(user.getInterests())
                .build();
    }

    /* =====================================================================================
     * LEADERBOARD RECALCULATION SYSTEM (ACTIVE MONTHLY)
     * ===================================================================================== */

    @Scheduled(cron = "0 0 * * * *") // Hər saat işləyir
    @Transactional
    public void recalculateActiveLeaderboard() {
        log.info("Starting recalculation for active (monthly) leaderboard");
        try {
            leaderboardEntryRepository.deleteAll();
            LocalDateTime startDate = LocalDateTime.now().minusMonths(1);

            List<User> activeUsers = userRepository.findAll();
            Map<String, Integer> userScores = new HashMap<>();

            for (User user : activeUsers) {
                int score = coinTransactionRepository.findAllByUserId(user.getId()).stream()
                        .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(startDate))
                        .map(CoinTransaction::getAmount)
                        .reduce(0, Integer::sum);

                if (score > 0) {
                    userScores.put(user.getId(), score);
                }
            }

            saveLeaderboardEntries(userScores);
            log.info("Successfully completed active leaderboard recalculation");
        } catch (Exception e) {
            log.error("Failed to recalculate active leaderboard", e);
        }
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

    /* =====================================================================================
     * MONTHLY WINNER BADGE SYSTEM
     * ===================================================================================== */

    @Scheduled(cron = "59 59 23 L * ?") // Hər ayın son günü saat 23:59:59-da işləyir
    @Transactional
    public void awardMonthlyWinnerBadges() {
        try {
            log.info("Starting monthly winner badge award process");
            YearMonth currentMonth = YearMonth.now();
            LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

            List<User> users = userRepository.findAll();
            Map<String, Integer> userScores = new HashMap<>();

            for (User user : users) {
                int score = coinTransactionRepository.findAllByUserId(user.getId()).stream()
                        .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(startOfMonth) && !t.getCreatedAt().isAfter(endOfMonth))
                        .map(CoinTransaction::getAmount)
                        .reduce(0, Integer::sum);

                if (score > 0) {
                    userScores.put(user.getId(), score);
                }
            }

            if (userScores.isEmpty()) {
                log.info("No active users found for this month to award badge.");
                return;
            }

            Map.Entry<String, Integer> topUser = userScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (topUser != null) {
                badgeRepository.findFirstByTypeAndIsActiveTrue(BadgeType.MONTHLY_WINNER)
                        .ifPresent(badge -> {
                            String specialKey = currentMonth.toString(); // e.g. "2026-06"
                            badgeService.awardBadgeToUser(topUser.getKey(), badge.getId(), specialKey);
                            log.info("Awarded MONTHLY_WINNER badge to user {} for month {}", topUser.getKey(), specialKey);
                        });
            }
        } catch (Exception e) {
            log.error("Failed to award monthly winner badges", e);
        }
    }
}

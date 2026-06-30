package com.ween.service;

import com.ween.entity.Badge;
import com.ween.entity.UserBadge;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.BadgeRepository;
import com.ween.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Transactional
    public void awardBadgeToUser(String userId, String badgeId, String specialKey) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge not found: " + badgeId));

        // Check if user already has this specific badge context
        if (specialKey != null && userBadgeRepository.existsByUserIdAndBadgeIdAndSpecialKey(userId, badgeId, specialKey)) {
            log.info("User {} already has badge {} with specialKey {}", userId, badgeId, specialKey);
            return;
        }

        UserBadge userBadge = UserBadge.builder()
                .userId(userId)
                .badge(badge)
                .earnedAt(LocalDateTime.now())
                .specialKey(specialKey)
                .build();

        userBadgeRepository.save(userBadge);
        log.info("Awarded badge {} to user {}", badge.getName(), userId);
    }
}

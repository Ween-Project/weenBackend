package com.ween.service;

import com.ween.dto.request.BadgeRequest;
import com.ween.dto.response.BadgeResponse;
import com.ween.dto.response.UserBadgeResponse;
import com.ween.entity.Badge;
import com.ween.entity.User;
import com.ween.entity.UserBadge;
import com.ween.enums.AchievementType;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.BadgeRepository;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.ReferralRepository;
import com.ween.repository.UserBadgeRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public BadgeResponse create(BadgeRequest request) {
        return create(request, null);
    }

    @Transactional
    public BadgeResponse create(BadgeRequest request, MultipartFile image) {
        validateRule(request);
        Badge badge = Badge.builder().build();
        apply(badge, request);
        if (image != null && !image.isEmpty()) {
            badge.setImageUrl(uploadBadgeImage(image));
        }
        Badge saved = badgeRepository.save(badge);
        evaluateAllUsersForBadge(saved);
        return toResponse(saved);
    }

    @Transactional
    public BadgeResponse update(String badgeId, BadgeRequest request) {
        return update(badgeId, request, null);
    }

    @Transactional
    public BadgeResponse update(String badgeId, BadgeRequest request, MultipartFile image) {
        validateRule(request);
        Badge badge = getBadge(badgeId);
        apply(badge, request);
        if (image != null && !image.isEmpty()) {
            badge.setImageUrl(uploadBadgeImage(image));
        }
        Badge saved = badgeRepository.save(badge);
        if (Boolean.TRUE.equals(saved.getIsActive())) {
            evaluateAllUsersForBadge(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public void deactivate(String badgeId) {
        Badge badge = getBadge(badgeId);
        badge.setIsActive(false);
        badgeRepository.save(badge);
    }

    @Transactional(readOnly = true)
    public Page<BadgeResponse> list(Pageable pageable) {
        return badgeRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UserBadgeResponse> getUserBadges(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return userBadgeRepository.findByUserId(userId).stream()
                .sorted((left, right) -> right.getEarnedAt().compareTo(left.getEarnedAt()))
                .map(this::toUserBadgeResponse)
                .toList();
    }

    @Transactional
    public void evaluateUserAchievements(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        for (Badge badge : badgeRepository.findByIsActiveTrue()) {
            if (badge.getAchievementType() != null && achievementValue(user, badge) >= badge.getAchievementThreshold()) {
                awardBadgeToUser(userId, badge.getId(), null);
            }
        }
    }

    @Transactional
    public void awardBadgeToUser(String userId, String badgeId, String specialKey) {
        Badge badge = getBadge(badgeId);
        boolean alreadyEarned = specialKey == null
                ? userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)
                : userBadgeRepository.existsByUserIdAndBadgeIdAndSpecialKey(userId, badgeId, specialKey);
        if (alreadyEarned) {
            return;
        }

        UserBadge userBadge = UserBadge.builder()
                .userId(userId)
                .badge(badge)
                .earnedAt(LocalDateTime.now())
                .specialKey(specialKey)
                .build();
        userBadgeRepository.save(userBadge);
        notificationService.createBadgeEarnedNotification(userId, badge.getName());
        log.info("Awarded badge {} to user {}", badge.getName(), userId);
    }

    private long achievementValue(User user, Badge badge) {
        return switch (badge.getAchievementType()) {
            case EVENT_ATTENDANCE_COUNT ->
                    eventRegistrationRepository.countByUserIdAndIsJoinedTrue(user.getId());
            case EVENT_CATEGORY_ATTENDANCE_COUNT ->
                    eventRegistrationRepository.countJoinedByUserIdAndEventCategory(user.getId(), badge.getEventCategory());
            case REFERRAL_COUNT -> referralRepository.countByReferrerId(user.getId());
            case PROFILE_COMPLETION -> isProfileComplete(user) ? 1 : 0;
            case COIN_BALANCE -> user.getWeenCoinBalance() == null ? 0 : user.getWeenCoinBalance();
        };
    }

    private boolean isProfileComplete(User user) {
        return present(user.getFullName())
                && present(user.getUniversity())
                && present(user.getMajor())
                && present(user.getBio())
                && present(user.getProfilePhotoUrl())
                && present(user.getSkills())
                && present(user.getInterests());
    }

    private boolean present(String value) {
        return value != null && !value.isBlank() && !"[]".equals(value);
    }

    private void evaluateAllUsersForBadge(Badge badge) {
        if (!Boolean.TRUE.equals(badge.getIsActive()) || badge.getAchievementType() == null) {
            return;
        }
        userRepository.findAll().forEach(user -> {
            if (achievementValue(user, badge) >= badge.getAchievementThreshold()) {
                awardBadgeToUser(user.getId(), badge.getId(), null);
            }
        });
    }

    private void validateRule(BadgeRequest request) {
        if (request.getAchievementType() == AchievementType.EVENT_CATEGORY_ATTENDANCE_COUNT
                && request.getEventCategory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event category is required for this achievement");
        }
    }

    private void apply(Badge badge, BadgeRequest request) {
        badge.setName(request.getName().trim());
        badge.setDescription(request.getDescription());
        badge.setType(request.getType());
        badge.setAchievementType(request.getAchievementType());
        badge.setAchievementThreshold(request.getAchievementThreshold());
        badge.setEventCategory(request.getAchievementType() == AchievementType.EVENT_CATEGORY_ATTENDANCE_COUNT
                ? request.getEventCategory() : null);
        badge.setPoints(request.getPoints() == null ? 0 : request.getPoints());
        if (request.getImageUrl() != null) {
            badge.setImageUrl(request.getImageUrl());
        }
        badge.setIsActive(request.getIsActive() == null || request.getIsActive());
    }

    private Badge getBadge(String badgeId) {
        return badgeRepository.findById(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge not found: " + badgeId));
    }

    private BadgeResponse toResponse(Badge badge) {
        return BadgeResponse.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .type(badge.getType())
                .achievementType(badge.getAchievementType())
                .achievementThreshold(badge.getAchievementThreshold())
                .eventCategory(badge.getEventCategory())
                .points(badge.getPoints())
                .imageUrl(badge.getImageUrl())
                .isActive(badge.getIsActive())
                .createdAt(badge.getCreatedAt())
                .build();
    }

    private String uploadBadgeImage(MultipartFile image) {
        try {
            return cloudinaryService.uploadFile(image, "badges");
        } catch (IOException e) {
            log.error("Failed to upload badge image to Cloudinary", e);
            throw new RuntimeException("Badge image upload failed", e);
        }
    }

    private UserBadgeResponse toUserBadgeResponse(UserBadge userBadge) {
        return UserBadgeResponse.builder()
                .id(userBadge.getId())
                .badge(toResponse(userBadge.getBadge()))
                .earnedAt(userBadge.getEarnedAt())
                .build();
    }
}

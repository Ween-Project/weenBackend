package com.ween.service;

import com.ween.dto.request.BadgeRequest;
import com.ween.dto.response.BadgeResponse;
import com.ween.dto.response.UserBadgeResponse;
import com.ween.entity.Badge;
import com.ween.entity.User;
import com.ween.entity.UserBadge;
import com.ween.enums.AchievementType;
import com.ween.enums.BadgeType;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.BadgeRepository;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.ReferralRepository;
import com.ween.repository.UserBadgeRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock private BadgeRepository badgeRepository;
    @Mock private UserBadgeRepository userBadgeRepository;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private ReferralRepository referralRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    void createBadgeThrowsExceptionWhenCategoryMissingForCategoryAttendance() {
        BadgeRequest request = new BadgeRequest();
        request.setName("Eco King");
        request.setAchievementType(AchievementType.EVENT_CATEGORY_ATTENDANCE_COUNT);
        request.setEventCategory(null); // Missing

        assertThatThrownBy(() -> badgeService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Event category is required");
    }

    @Test
    void createBadgeSavesAndEvaluatesUsers() throws IOException {
        BadgeRequest request = new BadgeRequest();
        request.setName("Eco King");
        request.setDescription("Eco Desc");
        request.setType(BadgeType.GOLD);
        request.setAchievementType(AchievementType.REFERRAL_COUNT);
        request.setAchievementThreshold(5);
        request.setIsActive(true);

        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadFile(eq(image), eq("badges"))).thenReturn("http://cloud-url");

        Badge badge = mock(Badge.class);
        when(badge.getId()).thenReturn("badge-123");
        when(badge.getIsActive()).thenReturn(true);
        when(badge.getAchievementType()).thenReturn(AchievementType.REFERRAL_COUNT);
        when(badge.getAchievementThreshold()).thenReturn(5);
        when(badgeRepository.save(any(Badge.class))).thenReturn(badge);
        when(badgeRepository.findById("badge-123")).thenReturn(Optional.of(badge));

        User user = mock(User.class);
        when(user.getId()).thenReturn("user-1");
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(referralRepository.countByReferrerId("user-1")).thenReturn(10L);

        BadgeResponse response = badgeService.create(request, image);

        verify(badgeRepository).save(any(Badge.class));
        verify(userBadgeRepository).save(any(UserBadge.class));
        verify(notificationService).createBadgeEarnedNotification(eq("user-1"), any());
    }

    @Test
    void deactivateBadgeSetsActiveToFalse() {
        Badge badge = mock(Badge.class);
        when(badgeRepository.findById("badge-1")).thenReturn(Optional.of(badge));

        badgeService.deactivate("badge-1");

        verify(badge).setIsActive(false);
        verify(badgeRepository).save(badge);
    }

    @Test
    void getUserBadgesReturnsSortedBadges() {
        when(userRepository.existsById("user-1")).thenReturn(true);
        UserBadge b1 = mock(UserBadge.class);
        UserBadge b2 = mock(UserBadge.class);
        when(b1.getEarnedAt()).thenReturn(LocalDateTime.now().minusDays(1));
        when(b2.getEarnedAt()).thenReturn(LocalDateTime.now());
        Badge badge = mock(Badge.class);
        when(b1.getBadge()).thenReturn(badge);
        when(b2.getBadge()).thenReturn(badge);

        when(userBadgeRepository.findByUserId("user-1")).thenReturn(List.of(b1, b2));

        List<UserBadgeResponse> result = badgeService.getUserBadges("user-1");

        assertThat(result).hasSize(2);
        // Order should be descending (b2 first since earnedAt is newer)
        assertThat(result.get(0).getEarnedAt()).isEqualTo(b2.getEarnedAt());
    }

    @Test
    void awardBadgeToUserSavesOnlyIfNew() {
        Badge badge = mock(Badge.class);
        when(badgeRepository.findById("badge-1")).thenReturn(Optional.of(badge));
        when(userBadgeRepository.existsByUserIdAndBadgeId("user-1", "badge-1")).thenReturn(true);

        badgeService.awardBadgeToUser("user-1", "badge-1", null);

        verify(userBadgeRepository, never()).save(any());
    }
}

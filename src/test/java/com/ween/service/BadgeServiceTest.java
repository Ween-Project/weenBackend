package com.ween.service;

import com.ween.repository.BadgeRepository;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.ReferralRepository;
import com.ween.repository.UserBadgeRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock BadgeRepository badgeRepository;
    @Mock UserBadgeRepository userBadgeRepository;
    @Mock EventRegistrationRepository eventRegistrationRepository;
    @Mock ReferralRepository referralRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @InjectMocks BadgeService badgeService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(badgeService).isNotNull();
    }
}

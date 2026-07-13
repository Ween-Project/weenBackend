package com.ween.service;

import com.ween.entity.Referral;
import com.ween.entity.User;
import com.ween.repository.ReferralRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock ReferralRepository referralRepository;
    @Mock UserRepository userRepository;
    @Mock CoinService coinService;
    @InjectMocks ReferralService referralService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(referralService).isNotNull();
    }

    @Test
    void processReferralAtSignupIgnoresInvalidReferralCode() {
        when(userRepository.findByReferralCode("INVALID")).thenReturn(Optional.empty());

        referralService.processReferralAtSignup("INVALID", "referred-123");

        verifyNoInteractions(referralRepository);
        verifyNoInteractions(coinService);
    }

    @Test
    void processReferralAtSignupIgnoresIfReferralAlreadyExists() {
        User referrer = User.builder().username("referrer").build();
        referrer.setId("referrer-123");

        when(userRepository.findByReferralCode("REF123")).thenReturn(Optional.of(referrer));
        when(referralRepository.findByReferrerIdAndReferredId("referrer-123", "referred-123"))
                .thenReturn(Optional.of(new Referral()));

        referralService.processReferralAtSignup("REF123", "referred-123");

        verify(referralRepository, never()).save(any());
        verifyNoInteractions(coinService);
    }

    @Test
    void processReferralAtSignupAwardsCoinsAndSavesReferralForValidNewReferral() {
        User referrer = User.builder().username("referrer").build();
        referrer.setId("referrer-123");

        when(userRepository.findByReferralCode("REF123")).thenReturn(Optional.of(referrer));
        when(referralRepository.findByReferrerIdAndReferredId("referrer-123", "referred-123"))
                .thenReturn(Optional.empty());

        referralService.processReferralAtSignup("REF123", "referred-123");

        verify(referralRepository).save(any(Referral.class));
        verify(coinService).awardReferralBonus("referrer-123", "referred-123");
        verify(coinService).awardReferredBonus("referred-123", "referrer-123");
    }
}

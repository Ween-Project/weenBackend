package com.ween.service;

import com.ween.entity.Referral;
import com.ween.entity.User;
import com.ween.exception.AlreadyExistsException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.ReferralRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    @Mock private ReferralRepository referralRepository;
    @Mock private UserRepository userRepository;
    @Mock private CoinService coinService;
    @InjectMocks private ReferralService referralService;

    private User referrer, referred;

    @BeforeEach
    void setUp() {
        referrer = User.builder().username("r").email("r@e.com").passwordHash("p")
                .fullName("R").referralCode("CODE1").build();
        referrer.setId("rid");
        referred = User.builder().username("d").email("d@e.com").passwordHash("p").fullName("D").build();
        referred.setId("did");
    }

    @Test @DisplayName("Create referral – success")
    void createReferral_success() {
        when(userRepository.findByReferralCode("CODE1")).thenReturn(Optional.of(referrer));
        when(userRepository.findById("did")).thenReturn(Optional.of(referred));
        when(referralRepository.findByReferrerIdAndReferredId("rid", "did")).thenReturn(Optional.empty());
        when(referralRepository.save(any())).thenAnswer(i -> { Referral r = i.getArgument(0); r.setId("refid"); return r; });

        Referral result = referralService.createReferral("CODE1", "did");
        assertThat(result.getReferrerId()).isEqualTo("rid");
        assertThat(result.getReferredId()).isEqualTo("did");
        assertThat(result.getCoinAwarded()).isFalse();
    }

    @Test @DisplayName("Create referral – invalid code throws")
    void createReferral_invalidCode() {
        when(userRepository.findByReferralCode("BAD")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> referralService.createReferral("BAD", "did"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Create referral – already exists throws")
    void createReferral_alreadyExists() {
        when(userRepository.findByReferralCode("CODE1")).thenReturn(Optional.of(referrer));
        when(userRepository.findById("did")).thenReturn(Optional.of(referred));
        when(referralRepository.findByReferrerIdAndReferredId("rid", "did"))
                .thenReturn(Optional.of(Referral.builder().build()));

        assertThatThrownBy(() -> referralService.createReferral("CODE1", "did"))
                .isInstanceOf(AlreadyExistsException.class);
    }

    @Test @DisplayName("Award referral coins – success")
    void awardReferralCoins_success() {
        Referral ref = Referral.builder().referrerId("rid").referredId("did").coinAwarded(false).build();
        ref.setId("refid");
        when(referralRepository.findById("refid")).thenReturn(Optional.of(ref));

        referralService.awardReferralCoins("refid");
        verify(coinService).awardReferralBonus("rid", "did");
        verify(coinService).credit(eq("did"), eq(100), any(), eq("rid"));
        assertThat(ref.getCoinAwarded()).isTrue();
    }

    @Test @DisplayName("Award referral coins – already awarded skips")
    void awardReferralCoins_alreadyAwarded() {
        Referral ref = Referral.builder().referrerId("rid").referredId("did").coinAwarded(true).build();
        ref.setId("refid");
        when(referralRepository.findById("refid")).thenReturn(Optional.of(ref));

        referralService.awardReferralCoins("refid");
        verify(coinService, never()).awardReferralBonus(any(), any());
    }

    @Test @DisplayName("Get referrer count")
    void getReferrerCount() {
        Referral r1 = Referral.builder().referrerId("rid").referredId("d1").build();
        Referral r2 = Referral.builder().referrerId("rid").referredId("d2").build();
        Referral r3 = Referral.builder().referrerId("other").referredId("d3").build();
        when(referralRepository.findAll()).thenReturn(List.of(r1, r2, r3));

        assertThat(referralService.getReferrerCount("rid")).isEqualTo(2);
    }

    @Test @DisplayName("Get successful referral count")
    void getSuccessfulReferralCount() {
        Referral r1 = Referral.builder().referrerId("rid").referredId("d1").coinAwarded(true).build();
        Referral r2 = Referral.builder().referrerId("rid").referredId("d2").coinAwarded(false).build();
        when(referralRepository.findAll()).thenReturn(List.of(r1, r2));

        assertThat(referralService.getSuccessfulReferralCount("rid")).isEqualTo(1);
    }

    @Test @DisplayName("Get total referral coins earned")
    void getTotalReferralCoinsEarned() {
        Referral r1 = Referral.builder().referrerId("rid").referredId("d1").coinAwarded(true).build();
        Referral r2 = Referral.builder().referrerId("rid").referredId("d2").coinAwarded(true).build();
        when(referralRepository.findAll()).thenReturn(List.of(r1, r2));

        assertThat(referralService.getTotalReferralCoinsEarned("rid")).isEqualTo(300); // 2 * 150
    }
}

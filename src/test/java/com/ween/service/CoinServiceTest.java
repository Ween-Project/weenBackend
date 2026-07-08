package com.ween.service;

import com.ween.entity.CoinTransaction;
import com.ween.entity.User;
import com.ween.enums.CoinReason;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.CoinTransactionMapper;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoinServiceTest {

    @Mock private CoinTransactionRepository coinTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CoinTransactionMapper coinTransactionMapper;
    @Mock private NotificationService notificationService;
    @Mock private BadgeService badgeService;

    private CoinService coinService;

    @BeforeEach
    void setUp() {
        coinService = new CoinService(
                coinTransactionRepository,
                userRepository,
                coinTransactionMapper,
                notificationService,
                badgeService);
    }

    @Test
    void getUserCoinBalanceReturnsStoredBalance() {
        User user = User.builder().weenCoinBalance(220).build();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThat(coinService.getUserCoinBalance("user-1")).isEqualTo(220);
    }

    @Test
    void getUserCoinBalanceFailsWhenUserMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coinService.getUserCoinBalance("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void awardSignupBonusCreditsOnlyFirstSignup() {
        User user = User.builder().weenCoinBalance(0).build();
        user.setId("user-1");
        when(coinTransactionRepository.countByUserIdAndReason("user-1", CoinReason.SIGNUP)).thenReturn(0L);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(coinTransactionRepository.save(any(CoinTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        coinService.awardSignupBonus("user-1");

        assertThat(user.getWeenCoinBalance()).isEqualTo(50);
        verify(notificationService).createCoinEarnedNotification("user-1", 50, "Signup bonus");
        verify(badgeService).evaluateUserAchievements("user-1");
    }

    @Test
    void awardSignupBonusSkipsWhenAlreadyAwarded() {
        when(coinTransactionRepository.countByUserIdAndReason("user-1", CoinReason.SIGNUP)).thenReturn(1L);

        coinService.awardSignupBonus("user-1");

        verify(userRepository, never()).save(any());
        verify(notificationService, never()).createCoinEarnedNotification(any(), any(), any());
    }
}

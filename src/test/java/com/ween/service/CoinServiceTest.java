package com.ween.service;

import com.ween.entity.CoinTransaction;
import com.ween.entity.User;
import com.ween.enums.CoinReason;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoinServiceTest {

    @Mock private CoinTransactionRepository coinTransactionRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private CoinService coinService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().username("u").email("u@e.com").passwordHash("p")
                .fullName("U").weenCoinBalance(500).build();
        testUser.setId("uid");
    }

    @Test @DisplayName("Credit – updates balance and creates transaction")
    void credit_success() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(coinTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CoinTransaction tx = coinService.credit("uid", 100, CoinReason.SIGNUP, null);
        assertThat(testUser.getWeenCoinBalance()).isEqualTo(600);
        assertThat(tx.getAmount()).isEqualTo(100);
    }

    @Test @DisplayName("Credit – user not found throws")
    void credit_notFound() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> coinService.credit("x", 10, CoinReason.SIGNUP, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Debit – success")
    void debit_success() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(coinTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CoinTransaction tx = coinService.debit("uid", 200, CoinReason.REGISTRATION, "eid");
        assertThat(testUser.getWeenCoinBalance()).isEqualTo(300);
        assertThat(tx.getAmount()).isEqualTo(-200);
    }

    @Test @DisplayName("Debit – insufficient balance throws")
    void debit_insufficient() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        assertThatThrownBy(() -> coinService.debit("uid", 1000, CoinReason.REGISTRATION, "e"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test @DisplayName("Award signup bonus – first time")
    void awardSignupBonus_firstTime() {
        when(coinTransactionRepository.countByUserIdAndReason("uid", CoinReason.SIGNUP)).thenReturn(0L);
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(coinTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coinService.awardSignupBonus("uid");
        verify(coinTransactionRepository).save(argThat(t -> t.getAmount() == 100));
    }

    @Test @DisplayName("Award signup bonus – already awarded, skips")
    void awardSignupBonus_alreadyAwarded() {
        when(coinTransactionRepository.countByUserIdAndReason("uid", CoinReason.SIGNUP)).thenReturn(1L);
        coinService.awardSignupBonus("uid");
        verify(userRepository, never()).save(any());
    }

    @Test @DisplayName("Award profile complete bonus – first time")
    void awardProfileComplete_firstTime() {
        when(coinTransactionRepository.countByUserIdAndReason("uid", CoinReason.PROFILE_COMPLETE)).thenReturn(0L);
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(coinTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coinService.awardProfileCompleteBonus("uid");
        verify(coinTransactionRepository).save(argThat(t -> t.getAmount() == 50));
    }

    @Test @DisplayName("Award profile complete bonus – already awarded, skips")
    void awardProfileComplete_alreadyAwarded() {
        when(coinTransactionRepository.countByUserIdAndReason("uid", CoinReason.PROFILE_COMPLETE)).thenReturn(1L);
        coinService.awardProfileCompleteBonus("uid");
        verify(userRepository, never()).save(any());
    }

    @Test @DisplayName("Leaderboard bonus – rank 1 gets 500")
    void leaderboardBonus_rank1() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(coinTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coinService.awardLeaderboardBonus("uid", 1);
        verify(coinTransactionRepository).save(argThat(t -> t.getAmount() == 500));
    }

    @Test @DisplayName("Leaderboard bonus – rank 5 gets 100")
    void leaderboardBonus_rank5() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        when(coinTransactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        coinService.awardLeaderboardBonus("uid", 5);
        verify(coinTransactionRepository).save(argThat(t -> t.getAmount() == 100));
    }

    @Test @DisplayName("Leaderboard bonus – rank 100 gets nothing")
    void leaderboardBonus_rank100() {
        coinService.awardLeaderboardBonus("uid", 100);
        verify(userRepository, never()).save(any());
    }

    @Test @DisplayName("Get user coin balance")
    void getUserCoinBalance() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        assertThat(coinService.getUserCoinBalance("uid")).isEqualTo(500);
    }

    @Test @DisplayName("Get user coin transactions")
    void getUserCoinTransactions() {
        CoinTransaction tx = CoinTransaction.builder().userId("uid").amount(10).reason(CoinReason.SIGNUP).build();
        when(coinTransactionRepository.findByUserId("uid")).thenReturn(List.of(tx));
        List<CoinTransaction> result = coinService.getUserCoinTransactions("uid", null);
        assertThat(result).hasSize(1);
    }
}

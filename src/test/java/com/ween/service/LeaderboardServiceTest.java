package com.ween.service;

import com.ween.entity.User;
import com.ween.repository.BadgeRepository;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.LeaderboardEntryRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock LeaderboardEntryRepository leaderboardEntryRepository;
    @Mock CoinTransactionRepository coinTransactionRepository;
    @Mock UserRepository userRepository;
    @Mock BadgeService badgeService;
    @Mock BadgeRepository badgeRepository;
    @InjectMocks LeaderboardService leaderboardService;

    @Test
    void mapsUsersToLeaderboardEntries() {
        User user = User.builder().username("ali").weenCoinBalance(100).build();
        user.setId("user-1");
        when(userRepository.findByRoleNotOrderByWeenCoinBalanceDesc(com.ween.enums.UserRole.ADMIN, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));

        assertThat(leaderboardService.getLeaderboardMapped(PageRequest.of(0, 10)).getContent())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.getRank()).isEqualTo(1);
                    assertThat(entry.getUsername()).isEqualTo("ali");
                    assertThat(entry.getCoins()).isEqualTo(100);
                });
    }
}

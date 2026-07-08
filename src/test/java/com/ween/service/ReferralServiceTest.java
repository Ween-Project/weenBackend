package com.ween.service;

import com.ween.repository.ReferralRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

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
}

package com.ween.service;

import com.ween.mapper.UserMapper;
import com.ween.repository.FollowRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock FollowRepository followRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @Mock UserMapper userMapper;
    @InjectMocks FollowService followService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(followService).isNotNull();
    }
}

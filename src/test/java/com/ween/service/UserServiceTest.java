package com.ween.service;

import com.ween.entity.User;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.UserMapper;
import com.ween.repository.ChatMessageRepository;
import com.ween.repository.FollowRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock CoinService coinService;
    @Mock FollowRepository followRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @InjectMocks UserService userService;

    @Test
    void getUserByIdReturnsExistingUser() {
        User user = User.builder().username("ali").build();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertThat(userService.getUserById("user-1")).isSameAs(user);
    }

    @Test
    void getUserByIdFailsWhenMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

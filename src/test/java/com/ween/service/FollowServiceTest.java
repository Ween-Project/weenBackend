package com.ween.service;

import com.ween.dto.response.PublicProfileResponse;
import com.ween.entity.Follow;
import com.ween.entity.User;
import com.ween.exception.AlreadyExistsException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.UserMapper;
import com.ween.repository.FollowRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FollowService followService;

    @Test
    void followUserThrowsExceptionWhenFollowingSelf() {
        assertThatThrownBy(() -> followService.followUser("user-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You cannot follow yourself");
    }

    @Test
    void followUserThrowsExceptionWhenUserNotFound() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.followUser("user-1", "user-2"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Current user not found");
    }

    @Test
    void followUserThrowsExceptionWhenTargetUserNotFound() {
        User user = mock(User.class);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.findById("user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.followUser("user-1", "user-2"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Target user not found");
    }

    @Test
    void followUserThrowsExceptionWhenAlreadyFollowing() {
        User follower = mock(User.class);
        User following = mock(User.class);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(follower));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(following));
        when(followRepository.existsByFollowerAndFollowing(follower, following)).thenReturn(true);

        assertThatThrownBy(() -> followService.followUser("user-1", "user-2"))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("You are already following this user");
    }

    @Test
    void followUserSavesRelationAndSendsNotification() {
        User follower = mock(User.class);
        when(follower.getUsername()).thenReturn("follower_username");
        User following = mock(User.class);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(follower));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(following));
        when(followRepository.existsByFollowerAndFollowing(follower, following)).thenReturn(false);

        followService.followUser("user-1", "user-2");

        verify(followRepository).save(any(Follow.class));
        verify(notificationService).createFollowNotification("user-2", "follower_username");
    }

    @Test
    void unfollowUserDeletesRelation() {
        User follower = mock(User.class);
        User following = mock(User.class);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(follower));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(following));

        followService.unfollowUser("user-1", "user-2");

        verify(followRepository).deleteByFollowerAndFollowing(follower, following);
    }

    @Test
    void getFollowersReturnsMappedPage() {
        User user = mock(User.class);
        User follower = mock(User.class);
        Follow follow = mock(Follow.class);
        when(follow.getFollower()).thenReturn(follower);
        PublicProfileResponse profileResponse = mock(PublicProfileResponse.class);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(followRepository.findByFollowing(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(follow)));
        when(userMapper.toPublicProfileResponse(follower)).thenReturn(profileResponse);

        Page<PublicProfileResponse> result = followService.getFollowers("user-1", 0, 10);

        assertThat(result.getContent()).containsExactly(profileResponse);
    }

    @Test
    void getFollowingReturnsMappedPage() {
        User user = mock(User.class);
        User following = mock(User.class);
        Follow follow = mock(Follow.class);
        when(follow.getFollowing()).thenReturn(following);
        PublicProfileResponse profileResponse = mock(PublicProfileResponse.class);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(followRepository.findByFollower(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(follow)));
        when(userMapper.toPublicProfileResponse(following)).thenReturn(profileResponse);

        Page<PublicProfileResponse> result = followService.getFollowing("user-1", 0, 10);

        assertThat(result.getContent()).containsExactly(profileResponse);
    }
}

package com.ween.service;

import com.ween.dto.response.PublicProfileResponse;
import com.ween.entity.Follow;
import com.ween.entity.User;
import com.ween.exception.AlreadyExistsException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.UserMapper;
import com.ween.repository.FollowRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    public void followUser(String currentUserId, String targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        User follower = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new AlreadyExistsException("You are already following this user");
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();

        followRepository.save(follow);
        log.info("User {} followed user {}", currentUserId, targetUserId);

        // Send Notification
        notificationService.createFollowNotification(targetUserId, follower.getUsername());
    }

    public void unfollowUser(String currentUserId, String targetUserId) {
        User follower = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        followRepository.deleteByFollowerAndFollowing(follower, following);
        log.info("User {} unfollowed user {}", currentUserId, targetUserId);
    }

    @Transactional(readOnly = true)
    public Page<PublicProfileResponse> getFollowers(String userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> follows = followRepository.findByFollowing(user, pageable);

        return follows.map(follow -> userMapper.toPublicProfileResponse(follow.getFollower()));
    }

    @Transactional(readOnly = true)
    public Page<PublicProfileResponse> getFollowing(String userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Follow> follows = followRepository.findByFollower(user, pageable);

        return follows.map(follow -> userMapper.toPublicProfileResponse(follow.getFollowing()));
    }
}

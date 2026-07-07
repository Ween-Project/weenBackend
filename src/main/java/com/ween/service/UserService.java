package com.ween.service;

import com.ween.dto.request.UpdateProfileRequest;
import com.ween.dto.response.PublicProfileResponse;
import com.ween.entity.User;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.UserMapper;
import com.ween.repository.UserRepository;
import com.ween.repository.FollowRepository;
import com.ween.repository.ChatMessageRepository;
import com.ween.enums.MessagePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CoinService coinService;
    private final FollowRepository followRepository;
    private final ChatMessageRepository chatMessageRepository;

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public User updateProfile(String userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getUniversity() != null) {
            user.setUniversity(request.getUniversity());
        }

        if (request.getMajor() != null) {
            user.setMajor(request.getMajor());
        }
        if (request.getCourse() != null) {
            user.setCourse(request.getCourse());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getLinkedinUrl() != null) {
            user.setLinkedinUrl(request.getLinkedinUrl());
        }
        if (request.getGithubUrl() != null) {
            user.setGithubUrl(request.getGithubUrl());
        }
        if (request.getInterests() != null) {
            user.setInterests(normalizeTags(request.getInterests()));
        }
        if (request.getSkills() != null) {
            user.setSkills(normalizeTags(request.getSkills()));
        }
        if (request.getProfilePhotoUrl() != null) {
            user.setProfilePhotoUrl(request.getProfilePhotoUrl());
        }
        if (request.getBannerUrl() != null) {
            user.setBannerUrl(request.getBannerUrl());
        }
        if (request.getMessagePermission() != null) {
            user.setMessagePermission(request.getMessagePermission());
        }

        User updated = userRepository.save(user);
        log.info("User profile updated: {}", userId);

        // Award profile complete bonus if all fields are filled
        if (isProfileComplete(updated)) {
            coinService.awardProfileCompleteBonus(userId);
        }

        return updated;
    }

    public boolean isProfileComplete(User user) {
        return user.getFullName() != null && !user.getFullName().isEmpty()
                && user.getBirthDate() != null
                && user.getPhone() != null && !user.getPhone().isEmpty()
                && user.getUniversity() != null && !user.getUniversity().isEmpty()
                && user.getMajor() != null && !user.getMajor().isEmpty()
                && user.getBio() != null && !user.getBio().isEmpty()
                && user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty();
    }

    public Integer getUserCoinBalance(String userId) {
        User user = getUserById(userId);
        return user.getWeenCoinBalance();
    }

    public PublicProfileResponse getPublicProfile(String username, String currentUserId) {
        User user = getUserByUsername(username);
        return enrichProfile(user, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<PublicProfileResponse> searchPublicProfiles(String query, String currentUserId, Pageable pageable) {
        return userRepository.searchPublicProfiles(query == null ? "" : query.trim(), pageable)
                .map(user -> enrichProfile(user, currentUserId));
    }

    private PublicProfileResponse enrichProfile(User user, String currentUserId) {
        PublicProfileResponse response = userMapper.toPublicProfileResponse(user);
        response.setFollowerCount(followRepository.countByFollowing(user));
        response.setFollowingCount(followRepository.countByFollower(user));
        if (currentUserId == null || currentUserId.equals(user.getId())) {
            response.setFollowing(false);
            response.setCanMessage(false);
            if (currentUserId == null) {
                response.setReferralCode(null);
            }
            return response;
        }
        response.setReferralCode(null);
        User current = userRepository.findById(currentUserId).orElse(null);
        boolean following = current != null && followRepository.existsByFollowerAndFollowing(current, user);
        response.setFollowing(following);
        MessagePermission permission = user.getMessagePermission() == null ? MessagePermission.EVERYONE : user.getMessagePermission();
        response.setCanMessage(chatMessageRepository.conversationExists(currentUserId, user.getId())
                || permission == MessagePermission.EVERYONE
                || (permission == MessagePermission.FOLLOWERS && following));
        return response;
    }

    private String normalizeTags(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }
        return "[\"" + trimmed.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(",", "\",\"") + "\"]";
    }
}

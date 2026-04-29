package com.ween.service;

import com.ween.dto.request.UpdateProfileRequest;
import com.ween.dto.response.PublicProfileResponse;
import com.ween.entity.User;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.UserMapper;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CoinService coinService;

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

        if (request.getBio() != null) {
            user.setBio(request.getBio());
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

    public PublicProfileResponse getPublicProfile(String username) {
        User user = getUserByUsername(username);
        return userMapper.toPublicProfileResponse(user);
    }


}
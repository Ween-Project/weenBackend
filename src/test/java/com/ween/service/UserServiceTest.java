package com.ween.service;

import com.ween.dto.request.UpdateProfileRequest;
import com.ween.dto.response.PublicProfileResponse;
import com.ween.entity.User;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.UserMapper;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private CoinService coinService;
    @InjectMocks private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().username("u").email("u@e.com").passwordHash("p")
                .fullName("Full Name").weenCoinBalance(100).build();
        testUser.setId("uid");
    }

    @Test @DisplayName("Get user by id – found")
    void getUserById_found() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        assertThat(userService.getUserById("uid").getUsername()).isEqualTo("u");
    }

    @Test @DisplayName("Get user by id – not found throws")
    void getUserById_notFound() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserById("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Get user by username – found")
    void getUserByUsername_found() {
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(testUser));
        assertThat(userService.getUserByUsername("u")).isEqualTo(testUser);
    }

    @Test @DisplayName("Get user by username – not found throws")
    void getUserByUsername_notFound() {
        when(userRepository.findByUsername("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserByUsername("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Update profile – partial fields")
    void updateProfile_partialFields() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Updated Name"); req.setPhone("+123");

        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User updated = userService.updateProfile("uid", req);
        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getPhone()).isEqualTo("+123");
    }

    @Test @DisplayName("Update profile – complete profile awards coin bonus")
    void updateProfile_completeProfile_awardsCoinBonus() {
        testUser.setFullName("F"); testUser.setBirthDate(LocalDate.of(2000,1,1));
        testUser.setPhone("+1"); testUser.setUniversity("U"); testUser.setMajor("M");
        testUser.setBio("B"); testUser.setProfilePhotoUrl("http://photo");

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("F");

        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        userService.updateProfile("uid", req);
        verify(coinService).awardProfileCompleteBonus("uid");
    }

    @Test @DisplayName("isProfileComplete – all fields present → true")
    void isProfileComplete_true() {
        testUser.setFullName("F"); testUser.setBirthDate(LocalDate.of(2000,1,1));
        testUser.setPhone("+1"); testUser.setUniversity("U"); testUser.setMajor("M");
        testUser.setBio("B"); testUser.setProfilePhotoUrl("http://photo");
        assertThat(userService.isProfileComplete(testUser)).isTrue();
    }

    @Test @DisplayName("isProfileComplete – missing field → false")
    void isProfileComplete_false() {
        testUser.setFullName("F");
        // missing other fields
        assertThat(userService.isProfileComplete(testUser)).isFalse();
    }

    @Test @DisplayName("Get user coin balance")
    void getUserCoinBalance() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        assertThat(userService.getUserCoinBalance("uid")).isEqualTo(100);
    }

    @Test @DisplayName("Get public profile")
    void getPublicProfile() {
        PublicProfileResponse resp = new PublicProfileResponse();
        when(userRepository.findByUsername("u")).thenReturn(Optional.of(testUser));
        when(userMapper.toPublicProfileResponse(testUser)).thenReturn(resp);
        assertThat(userService.getPublicProfile("u")).isEqualTo(resp);
    }
}

package com.ween.dto.response;

import com.ween.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String fullName;
    private LocalDate birthDate;
    private String phone;
    private String university;
    private String major;
    private String bio;
    private String profilePhotoUrl;
    private Integer weenCoinBalance;
    private UserRole role;
    private Boolean isEmailVerified;
    private String linkedinUrl;
    private String githubUrl;
    private String interests;
    private String skills;
    private String referralCode;
    private String category;
    private String qrToken;
}
package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProfileResponse {
    private String id;
    private String username;
    private String fullName;
    private LocalDate birthDate;
    private String university;
    private String major;
    private String bio;
    private String profilePhotoUrl;
    private Integer weenCoinBalance;
    private String linkedinUrl;
    private String githubUrl;
    private String interests;
    private String skills;
}
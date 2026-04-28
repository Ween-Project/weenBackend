package com.ween.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String fullName;
    private LocalDate birthDate;
    private String phone;
    private String university;
    private String major;
    private String bio;
    private String linkedinUrl;
    private String githubUrl;
    private String interests;
    private String skills;
}
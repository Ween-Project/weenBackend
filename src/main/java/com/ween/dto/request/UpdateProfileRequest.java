package com.ween.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import jakarta.validation.constraints.Size;
import com.ween.enums.MessagePermission;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String fullName;
    private LocalDate birthDate;
    private String phone;
    private String university;
    private String major;
    @Size(max = 10, message = "Course must not exceed 10 characters")
    private String course;
    private String bio;
    private String linkedinUrl;
    private String githubUrl;
    private String interests;
    private String skills;

    private MessagePermission messagePermission;
}
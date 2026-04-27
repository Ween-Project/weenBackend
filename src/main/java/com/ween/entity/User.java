package com.ween.entity;

import com.ween.enums.UserRole;
import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_referral_code", columnList = "referral_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 120, nullable = false)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String university;

    @Column(length = 100)
    private String major;

    @Column(length = 10)
    private String course;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "ween_coin_balance")
    private Integer weenCoinBalance = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.VOLUNTEER;

    @Column(name = "is_email_verified")
    private Boolean isEmailVerified = false;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    @Column(name = "github_url", length = 300)
    private String githubUrl;

    @Column(columnDefinition = "JSON")
    private String interests;

    @Column(columnDefinition = "JSON")
    private String skills;

    @Column(name = "referral_code", length = 20, unique = true)
    private String referralCode;

    @Column(name = "is_banned")
    private Boolean banned = false;

    @Column(name = "ban_reason", columnDefinition = "TEXT")
    private String banReason;
}
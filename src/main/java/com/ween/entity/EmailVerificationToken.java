package com.ween.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_email_verification_user_id", columnList = "user_id"),
        @Index(name = "idx_email_verification_token", columnList = "token")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Column(name = "token", length = 180, nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", columnDefinition = "DATETIME(6)", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime verifiedAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;
}

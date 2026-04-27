package com.ween.entity;

import com.ween.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_org_username", columnList = "username"),
    @Index(name = "idx_org_email", columnList = "email"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends BaseEntity {

    @Column(length = 50, nullable = false, unique = true)
    private String username;

    @Column(length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(length = 200, nullable = false)
    private String organizationName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.VOLUNTEER;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 300)
    private String website;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verification_note", columnDefinition = "TEXT")
    private String verificationNote;

    public void setVerified(boolean b) {
        this.isVerified = b;
    }
}

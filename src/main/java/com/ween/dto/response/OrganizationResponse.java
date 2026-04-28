package com.ween.dto.response;

import com.ween.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private String id;
    private String username;
    private String organizationName;
    private String description;
    private String logoUrl;
    private String email;
    private String website;
    private UserRole role;
    private Boolean isVerified;
    private String verificationNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
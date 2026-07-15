package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerResponse {
    private String organizerId;
    private String userId;
    private String fullName;
    private String email;
    private String username;
    private String profilePhotoUrl;
}

package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAuthorResponse {
    private String id;
    private String username;
    private String fullName;
    private String profilePhotoUrl;
    private String accountType;
}

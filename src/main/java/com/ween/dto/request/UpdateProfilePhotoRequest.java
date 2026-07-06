package com.ween.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfilePhotoRequest {
    // The URL of the new profile photo to update for the user.
    private String imageUrl;
}
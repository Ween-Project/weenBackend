package com.ween.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePostRequest {

    @NotBlank(message = "Post content is required")
    @Size(max = 5000, message = "Post content must be at most 5000 characters")
    private String content;

    @Size(max = 500, message = "Media URL must be at most 500 characters")
    private String mediaUrl;
}

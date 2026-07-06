package com.ween.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddPostCommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(max = 2000, message = "Comment content must be at most 2000 characters")
    private String content;
}

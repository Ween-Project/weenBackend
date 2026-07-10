package com.ween.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {

    @Size(max = 5000, message = "Post content must be at most 5000 characters")
    private String content;

}

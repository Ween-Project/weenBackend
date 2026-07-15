package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCommentResponse {
    private String id;
    private String postId;
    private PostAuthorResponse author;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String parentCommentId;
    private int likeCount;
    private boolean isLikedByMe;
    private java.util.List<PostCommentResponse> replies;
}

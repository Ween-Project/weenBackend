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
public class PostResponse {
    private String id;
    private PostAuthorResponse author;
    private String content;
    private String mediaUrl;
    private long likeCount;
    private long commentCount;
    private long saveCount;
    private long repostCount;
    private boolean likedByMe;
    private boolean savedByMe;
    private boolean repostedByMe;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

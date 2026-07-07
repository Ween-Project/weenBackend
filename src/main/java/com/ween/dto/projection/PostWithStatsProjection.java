package com.ween.dto.projection;

import com.ween.entity.Post;

public interface PostWithStatsProjection {
    Post getPost();
    Long getLikeCount();
    Long getCommentCount();
    Long getSaveCount();
    Long getRepostCount();
    Boolean getLikedByMe();
    Boolean getSavedByMe();
    Boolean getRepostedByMe();
}

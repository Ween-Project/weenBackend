package com.ween.repository;

import com.ween.entity.Post;
import com.ween.entity.PostLike;
import com.ween.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, String> {
    boolean existsByPostAndUser(Post post, User user);
    long countByPost(Post post);
    void deleteByPostAndUser(Post post, User user);
    void deleteByPost(Post post);

    @Query("""
        SELECT pl.post AS post,
               (SELECT COUNT(l) FROM PostLike l WHERE l.post = pl.post) AS likeCount,
               (SELECT COUNT(c) FROM PostComment c WHERE c.post = pl.post) AS commentCount,
               (SELECT COUNT(s) FROM PostSave s WHERE s.post = pl.post) AS saveCount,
               (SELECT COUNT(r) FROM PostRepost r WHERE r.originalPost = pl.post) AS repostCount,
               (SELECT CASE WHEN COUNT(l2) > 0 THEN true ELSE false END FROM PostLike l2 WHERE l2.post = pl.post AND l2.user.id = :currentUserId) AS likedByMe,
               (SELECT CASE WHEN COUNT(s2) > 0 THEN true ELSE false END FROM PostSave s2 WHERE s2.post = pl.post AND s2.user.id = :currentUserId) AS savedByMe,
               (SELECT CASE WHEN COUNT(r2) > 0 THEN true ELSE false END FROM PostRepost r2 WHERE r2.originalPost = pl.post AND r2.user.id = :currentUserId) AS repostedByMe
        FROM PostLike pl
        WHERE pl.user = :user
        """)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"post", "post.userAuthor", "post.organizationAuthor"})
    org.springframework.data.domain.Page<com.ween.dto.projection.PostWithStatsProjection> findLikedPostsWithStats(
            @org.springframework.data.repository.query.Param("user") User user, 
            @org.springframework.data.repository.query.Param("currentUserId") String currentUserId, 
            org.springframework.data.domain.Pageable pageable);
}

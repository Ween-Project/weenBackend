package com.ween.repository;

import com.ween.entity.Post;
import com.ween.entity.PostRepost;
import com.ween.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepostRepository extends JpaRepository<PostRepost, String> {
    boolean existsByOriginalPostAndUser(Post originalPost, User user);
    long countByOriginalPost(Post originalPost);
    void deleteByOriginalPostAndUser(Post originalPost, User user);
    void deleteByOriginalPost(Post originalPost);

    @Query("""
        SELECT pr.originalPost AS post,
               (SELECT COUNT(l) FROM PostLike l WHERE l.post = pr.originalPost) AS likeCount,
               (SELECT COUNT(c) FROM PostComment c WHERE c.post = pr.originalPost) AS commentCount,
               (SELECT COUNT(s) FROM PostSave s WHERE s.post = pr.originalPost) AS saveCount,
               (SELECT COUNT(r) FROM PostRepost r WHERE r.originalPost = pr.originalPost) AS repostCount,
               (SELECT CASE WHEN COUNT(l2) > 0 THEN true ELSE false END FROM PostLike l2 WHERE l2.post = pr.originalPost AND l2.user.id = :currentUserId) AS likedByMe,
               (SELECT CASE WHEN COUNT(s2) > 0 THEN true ELSE false END FROM PostSave s2 WHERE s2.post = pr.originalPost AND s2.user.id = :currentUserId) AS savedByMe,
               (SELECT CASE WHEN COUNT(r2) > 0 THEN true ELSE false END FROM PostRepost r2 WHERE r2.originalPost = pr.originalPost AND r2.user.id = :currentUserId) AS repostedByMe
        FROM PostRepost pr
        WHERE pr.user = :user
        """)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"post", "post.userAuthor", "post.organizationAuthor"})
    org.springframework.data.domain.Page<com.ween.dto.projection.PostWithStatsProjection> findRepostedPostsWithStats(
            @org.springframework.data.repository.query.Param("user") User user, 
            @org.springframework.data.repository.query.Param("currentUserId") String currentUserId, 
            org.springframework.data.domain.Pageable pageable);
}

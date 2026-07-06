package com.ween.repository;

import com.ween.entity.Post;
import com.ween.entity.PostSave;
import com.ween.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ween.dto.projection.PostWithStatsProjection;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface PostSaveRepository extends JpaRepository<PostSave, String> {

    boolean existsByPostAndUser(Post post, User user);

    long countByPost(Post post);

    void deleteByPostAndUser(Post post, User user);

    void deleteByPost(Post post);

    @EntityGraph(attributePaths = {"post", "post.userAuthor", "post.organizationAuthor"})
    Page<PostSave> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("""
        SELECT ps.post AS post,
               (SELECT COUNT(l) FROM PostLike l WHERE l.post = ps.post) AS likeCount,
               (SELECT COUNT(c) FROM PostComment c WHERE c.post = ps.post) AS commentCount,
               (SELECT COUNT(s) FROM PostSave s WHERE s.post = ps.post) AS saveCount,
               (SELECT COUNT(r) FROM PostRepost r WHERE r.originalPost = ps.post) AS repostCount,
               (SELECT CASE WHEN COUNT(l2) > 0 THEN true ELSE false END FROM PostLike l2 WHERE l2.post = ps.post AND l2.user.id = :currentUserId) AS likedByMe,
               (SELECT CASE WHEN COUNT(s2) > 0 THEN true ELSE false END FROM PostSave s2 WHERE s2.post = ps.post AND s2.user.id = :currentUserId) AS savedByMe,
               (SELECT CASE WHEN COUNT(r2) > 0 THEN true ELSE false END FROM PostRepost r2 WHERE r2.originalPost = ps.post AND r2.user.id = :currentUserId) AS repostedByMe
        FROM PostSave ps
        WHERE ps.user = :user
        """)
    @EntityGraph(attributePaths = {"post", "post.userAuthor", "post.organizationAuthor"})
    Page<PostWithStatsProjection> findSavedPostsWithStats(@Param("user") User user, @Param("currentUserId") String currentUserId, Pageable pageable);
}

package com.ween.repository;

import com.ween.entity.Post;
import com.ween.entity.User;
import com.ween.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ween.dto.projection.PostWithStatsProjection;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    @EntityGraph(attributePaths = {"userAuthor", "organizationAuthor"})
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :search, '%'))")
    @EntityGraph(attributePaths = {"userAuthor", "organizationAuthor"})
    Page<Post> searchPosts(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = {"userAuthor", "organizationAuthor"})
    Page<Post> findByUserAuthorOrderByCreatedAtDesc(User userAuthor, Pageable pageable);

    @Query("""
        SELECT p AS post,
               (SELECT COUNT(l) FROM PostLike l WHERE l.post = p) AS likeCount,
               (SELECT COUNT(c) FROM PostComment c WHERE c.post = p) AS commentCount,
               (SELECT COUNT(s) FROM PostSave s WHERE s.post = p) AS saveCount,
               (SELECT COUNT(r) FROM PostRepost r WHERE r.originalPost = p) AS repostCount,
               (SELECT CASE WHEN COUNT(l2) > 0 THEN true ELSE false END FROM PostLike l2 WHERE l2.post = p AND l2.user.id = :currentUserId) AS likedByMe,
               (SELECT CASE WHEN COUNT(s2) > 0 THEN true ELSE false END FROM PostSave s2 WHERE s2.post = p AND s2.user.id = :currentUserId) AS savedByMe,
               (SELECT CASE WHEN COUNT(r2) > 0 THEN true ELSE false END FROM PostRepost r2 WHERE r2.originalPost = p AND r2.user.id = :currentUserId) AS repostedByMe
        FROM Post p
        """)
    @EntityGraph(attributePaths = {"userAuthor", "organizationAuthor"})
    Page<PostWithStatsProjection> findAllPostsWithStats(@Param("currentUserId") String currentUserId, Pageable pageable);

    @Query("""
        SELECT p AS post,
               (SELECT COUNT(l) FROM PostLike l WHERE l.post = p) AS likeCount,
               (SELECT COUNT(c) FROM PostComment c WHERE c.post = p) AS commentCount,
               (SELECT COUNT(s) FROM PostSave s WHERE s.post = p) AS saveCount,
               (SELECT COUNT(r) FROM PostRepost r WHERE r.originalPost = p) AS repostCount,
               (SELECT CASE WHEN COUNT(l2) > 0 THEN true ELSE false END FROM PostLike l2 WHERE l2.post = p AND l2.user.id = :currentUserId) AS likedByMe,
               (SELECT CASE WHEN COUNT(s2) > 0 THEN true ELSE false END FROM PostSave s2 WHERE s2.post = p AND s2.user.id = :currentUserId) AS savedByMe,
               (SELECT CASE WHEN COUNT(r2) > 0 THEN true ELSE false END FROM PostRepost r2 WHERE r2.originalPost = p AND r2.user.id = :currentUserId) AS repostedByMe
        FROM Post p
        WHERE p.userAuthor = :author
        """)
    @EntityGraph(attributePaths = {"userAuthor", "organizationAuthor"})
    Page<PostWithStatsProjection> findPostsWithStatsByAuthor(@Param("author") User author, @Param("currentUserId") String currentUserId, Pageable pageable);

    @Query("""
        SELECT p AS post,
               (SELECT COUNT(l) FROM PostLike l WHERE l.post = p) AS likeCount,
               (SELECT COUNT(c) FROM PostComment c WHERE c.post = p) AS commentCount,
               (SELECT COUNT(s) FROM PostSave s WHERE s.post = p) AS saveCount,
               (SELECT COUNT(r) FROM PostRepost r WHERE r.originalPost = p) AS repostCount,
               (SELECT CASE WHEN COUNT(l2) > 0 THEN true ELSE false END FROM PostLike l2 WHERE l2.post = p AND l2.user.id = :currentUserId) AS likedByMe,
               (SELECT CASE WHEN COUNT(s2) > 0 THEN true ELSE false END FROM PostSave s2 WHERE s2.post = p AND s2.user.id = :currentUserId) AS savedByMe,
               (SELECT CASE WHEN COUNT(r2) > 0 THEN true ELSE false END FROM PostRepost r2 WHERE r2.originalPost = p AND r2.user.id = :currentUserId) AS repostedByMe
        FROM Post p
        WHERE p.organizationAuthor = :organization
        """)

    @EntityGraph(attributePaths = {"userAuthor", "organizationAuthor"})
    Page<PostWithStatsProjection> findPostsWithStatsByOrganization(
            @Param("organization") Organization organization,
            @Param("currentUserId") String currentUserId,
            Pageable pageable);

    @Query("""
        SELECT p AS post,
               (SELECT COUNT(l) FROM PostLike l WHERE l.post = p) AS likeCount,
               (SELECT COUNT(c) FROM PostComment c WHERE c.post = p) AS commentCount,
               (SELECT COUNT(s) FROM PostSave s WHERE s.post = p) AS saveCount,
               (SELECT COUNT(r) FROM PostRepost r WHERE r.originalPost = p) AS repostCount,
               (SELECT CASE WHEN COUNT(l2) > 0 THEN true ELSE false END FROM PostLike l2 WHERE l2.post = p AND l2.user.id = :currentUserId) AS likedByMe,
               (SELECT CASE WHEN COUNT(s2) > 0 THEN true ELSE false END FROM PostSave s2 WHERE s2.post = p AND s2.user.id = :currentUserId) AS savedByMe,
               (SELECT CASE WHEN COUNT(r2) > 0 THEN true ELSE false END FROM PostRepost r2 WHERE r2.originalPost = p AND r2.user.id = :currentUserId) AS repostedByMe
        FROM Post p
        WHERE p.userAuthor IN :followedUsers
        """)
    @EntityGraph(attributePaths = {"userAuthor", "organizationAuthor"})
    Page<PostWithStatsProjection> findPostsWithStatsByFollowedUsers(
            @Param("followedUsers") java.util.Collection<User> followedUsers,
            @Param("currentUserId") String currentUserId,
            Pageable pageable);

    @Query("""
        SELECT p AS post,
               (SELECT COUNT(l) FROM PostLike l WHERE l.post = p) AS likeCount,
               (SELECT COUNT(c) FROM PostComment c WHERE c.post = p) AS commentCount,
               (SELECT COUNT(s) FROM PostSave s WHERE s.post = p) AS saveCount,
               (SELECT COUNT(r) FROM PostRepost r WHERE r.originalPost = p) AS repostCount,
               (SELECT CASE WHEN COUNT(l2) > 0 THEN true ELSE false END FROM PostLike l2 WHERE l2.post = p AND l2.user.id = :currentUserId) AS likedByMe,
               (SELECT CASE WHEN COUNT(s2) > 0 THEN true ELSE false END FROM PostSave s2 WHERE s2.post = p AND s2.user.id = :currentUserId) AS savedByMe,
               (SELECT CASE WHEN COUNT(r2) > 0 THEN true ELSE false END FROM PostRepost r2 WHERE r2.originalPost = p AND r2.user.id = :currentUserId) AS repostedByMe
        FROM Post p
        WHERE p.id = :postId
        """)
    @EntityGraph(attributePaths = {"userAuthor", "organizationAuthor"})
    Optional<PostWithStatsProjection> findPostWithStatsById(@Param("postId") String postId, @Param("currentUserId") String currentUserId);
}

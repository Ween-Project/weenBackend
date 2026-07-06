package com.ween.repository;

import com.ween.entity.Post;
import com.ween.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, String> {

    @EntityGraph(attributePaths = "author")
    Page<PostComment> findByPostOrderByCreatedAtAsc(Post post, Pageable pageable);

    long countByPost(Post post);

    void deleteByPost(Post post);
}

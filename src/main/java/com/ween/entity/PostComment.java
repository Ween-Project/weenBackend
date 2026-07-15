package com.ween.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "post_comments", indexes = {
        @Index(name = "idx_post_comments_post", columnList = "post_id"),
        @Index(name = "idx_post_comments_author", columnList = "author_id"),
        @Index(name = "idx_post_comments_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PostComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<PostComment> replies = new java.util.ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "post_comment_likes",
        joinColumns = @JoinColumn(name = "comment_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private java.util.Set<User> likedBy = new java.util.HashSet<>();

}

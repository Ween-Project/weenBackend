package com.ween.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "post_likes", uniqueConstraints = {
        @UniqueConstraint(name = "uq_post_likes_post_user", columnNames = {"post_id", "user_id"})
}, indexes = {
        @Index(name = "idx_post_likes_post", columnList = "post_id"),
        @Index(name = "idx_post_likes_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

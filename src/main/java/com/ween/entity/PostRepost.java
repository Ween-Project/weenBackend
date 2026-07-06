package com.ween.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "post_reposts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_post_reposts_post_user", columnNames = {"original_post_id", "user_id"})
}, indexes = {
        @Index(name = "idx_post_reposts_post", columnList = "original_post_id"),
        @Index(name = "idx_post_reposts_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRepost extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_post_id", nullable = false)
    private Post originalPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

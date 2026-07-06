package com.ween.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "post_saves", uniqueConstraints = {
        @UniqueConstraint(name = "uq_post_saves_post_user", columnNames = {"post_id", "user_id"})
}, indexes = {
        @Index(name = "idx_post_saves_post", columnList = "post_id"),
        @Index(name = "idx_post_saves_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostSave extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}

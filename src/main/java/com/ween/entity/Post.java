package com.ween.entity;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_author", columnList = "author_id"),
        @Index(name = "idx_posts_organization_author", columnList = "organization_author_id"),
        @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User userAuthor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_author_id")
    private Organization organizationAuthor;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean hidden = false;

}

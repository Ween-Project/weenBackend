package com.ween.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "actor_id", length = 36)
    private String actorId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "target_id", length = 36)
    private String targetId;

    @Column(name = "target_name", length = 200)
    private String targetName;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}

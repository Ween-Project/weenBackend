package com.ween.entity;

import com.ween.enums.EventCategory;
import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "certificates", indexes = {
        @Index(name = "idx_cert_user_id", columnList = "user_id"),
        @Index(name = "idx_cert_event_id", columnList = "event_id"),
        @Index(name = "idx_cert_number", columnList = "certificate_number", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Column(name = "event_id", columnDefinition = "CHAR(36)", nullable = false)
    private String eventId;

    @Column(name = "certificate_number", length = 30, unique = true)
    private String certificateNumber;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_type")
    private EventCategory templateType = EventCategory.EDUCATION;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;
}

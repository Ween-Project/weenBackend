package com.ween.entity;

import lombok.*;
import jakarta.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "referrals", indexes = {
        @Index(name = "idx_referrer_id", columnList = "referrer_id"),
        @Index(name = "idx_referred_id", columnList = "referred_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral extends BaseEntity {

    @Column(name = "referrer_id", columnDefinition = "CHAR(36)", nullable = false)
    private String referrerId;

    @Column(name = "referred_id", columnDefinition = "CHAR(36)", nullable = false)
    private String referredId;

    @Column(name = "coin_awarded")
    @Builder.Default
    private Boolean coinAwarded = false;
}
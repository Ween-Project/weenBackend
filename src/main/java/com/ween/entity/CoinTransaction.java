package com.ween.entity;

import com.ween.enums.CoinReason;
import lombok.*;
import jakarta.persistence.*;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "coin_transactions", indexes = {
        @Index(name = "idx_coin_user_id", columnList = "user_id"),
        @Index(name = "idx_coin_reason", columnList = "reason")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinTransaction extends BaseEntity {

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    private String userId;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoinReason reason;


}

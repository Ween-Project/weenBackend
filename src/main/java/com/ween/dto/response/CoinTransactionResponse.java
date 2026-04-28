package com.ween.dto.response;

import com.ween.enums.CoinReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinTransactionResponse {
    private String id;
    private Integer amount;
    private CoinReason reason;
    private String relatedEntityId;
    private LocalDateTime createdAt;
}

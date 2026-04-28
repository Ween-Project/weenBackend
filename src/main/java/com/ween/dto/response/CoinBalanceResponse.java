package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinBalanceResponse {
    private String userId;
    private Integer balance;
    private Long totalEarned;
}

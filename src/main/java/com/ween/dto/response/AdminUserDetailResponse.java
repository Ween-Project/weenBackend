package com.ween.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserDetailResponse {
    private UserResponse user;
    private List<CertificateResponse> certificates;
    private List<UserBadgeResponse> badges;
    private List<EventResponse> eventsAttended;
    private List<EventResponse> eventsOrganized;
    private List<CoinTransactionResponse> coinTransactions;
}

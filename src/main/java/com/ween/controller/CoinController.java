package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.CoinTransactionResponse;
import com.ween.security.SecurityUtil;
import com.ween.service.CoinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/coins")
@RequiredArgsConstructor
@Tag(name = "Coins", description = "Coin balance, transactions, and leaderboard endpoints")
public class CoinController {

    private final CoinService coinService;
    private final SecurityUtil securityUtil;

    @GetMapping("/balance")
    @Operation(summary = "Get coin balance", description = "Get current user's coin balance")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Integer>> getCoinBalance() {
        try {
            String userId = securityUtil.getCurrentUserId();
            Integer response = coinService.getUserCoinBalance(userId);
            return ResponseEntity.ok(ApiResponse.ok(response, "Balance retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve coin balance for user",  e);
            throw e;
        }
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get coin transactions", description = "Get pageable list of user's coin transactions")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Page<CoinTransactionResponse>>> getCoinTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            String userId = securityUtil.getCurrentUserId();
            Page<CoinTransactionResponse> response = coinService.getUserCoinTransactions(userId, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Transactions retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve coin transactions for user", e);
            throw e;
        }
    }


}
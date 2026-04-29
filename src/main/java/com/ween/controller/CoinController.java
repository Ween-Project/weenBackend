package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.LeaderboardEntryResponse;
import com.ween.entity.CoinTransaction;
import com.ween.enums.LeaderboardPeriod;
import com.ween.enums.LeaderboardScope;
import com.ween.service.CoinService;
import com.ween.service.LeaderboardService;
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
    private final LeaderboardService leaderboardService;

    @GetMapping("/balance")
    @Operation(summary = "Get coin balance", description = "Get current user's coin balance")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Integer>> getCoinBalance() {
        try {
            String userId = getCurrentUserId();
            Integer response = coinService.getUserCoinBalance(userId);
            return ResponseEntity.ok(ApiResponse.ok(response, "Balance retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve coin balance for user: {}", getCurrentUserId(), e);
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
    public ResponseEntity<ApiResponse<List<CoinTransaction>>> getCoinTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        try {
            String userId = getCurrentUserId();
            List<CoinTransaction> response = coinService.getUserCoinTransactions(userId, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Transactions retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve coin transactions for user: {}", getCurrentUserId(), e);
            throw e;
        }
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get leaderboard", description = "Get pageable leaderboard with optional period and scope filters")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<LeaderboardEntryResponse>>> getLeaderboard(
            @Parameter(description = "Leaderboard period (default: MONTHLY)") @RequestParam(required = false, defaultValue = "MONTHLY") LeaderboardPeriod period,
            @Parameter(description = "Leaderboard scope (default: GLOBAL)") @RequestParam(required = false, defaultValue = "GLOBAL") LeaderboardScope scope,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            Page<LeaderboardEntryResponse> response = leaderboardService.getLeaderboardMapped(period, scope, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Leaderboard retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve leaderboard for period: {}, scope: {}", period, scope, e);
            throw e;
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        return (String) authentication.getPrincipal();
    }
}
package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.LeaderboardEntryDto;
import com.ween.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Leaderboard endpoints")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    @Operation(summary = "Get leaderboard", description = "Get pageable leaderboard with optional period filter")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<LeaderboardEntryDto>>> getLeaderboard(
            @Parameter(description = "Leaderboard period (ACTIVE or ALL_TIME)") @RequestParam(required = false, defaultValue = "ACTIVE") String period,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            Page<LeaderboardEntryDto> response = leaderboardService.getLeaderboardMapped(period, pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Leaderboard retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve leaderboard for period: {}", period, e);
            throw e;
        }
    }
}

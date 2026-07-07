package com.ween.controller;

import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.LeaderboardEntryDto;
import com.ween.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Leaderboard endpoints")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    @Operation(summary = "Get leaderboard", description = "Get the pageable global leaderboard")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<LeaderboardEntryDto>>> getLeaderboard(
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            Page<LeaderboardEntryDto> response = leaderboardService.getLeaderboardMapped(pageable);
            return ResponseEntity.ok(ApiResponse.ok(response, "Leaderboard retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve leaderboard", e);
            throw e;
        }
    }
}

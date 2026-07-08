package com.ween.controller;

import com.ween.dto.response.LeaderboardEntryDto;
import com.ween.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaderboardControllerTest {

    private LeaderboardService leaderboardService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        leaderboardService = mock(LeaderboardService.class);
        mockMvc = standaloneSetup(new LeaderboardController(leaderboardService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getLeaderboardReturnsMappedEntries() throws Exception {
        LeaderboardEntryDto entry = LeaderboardEntryDto.builder()
                .rank(1)
                .userId("user-1")
                .username("ali")
                .coins(500)
                .build();
        when(leaderboardService.getLeaderboardMapped(any()))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/api/v1/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].rank").value(1))
                .andExpect(jsonPath("$.data.content[0].coins").value(500));
    }
}

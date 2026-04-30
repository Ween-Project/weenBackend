package com.ween.controller;

import com.ween.entity.CoinTransaction;
import com.ween.security.JwtUtil;
import com.ween.service.CoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import com.ween.service.LeaderboardService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CoinController.class)
@AutoConfigureMockMvc(addFilters = false)
class CoinControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CoinService coinService;
    @MockBean private LeaderboardService leaderboardService;
    @MockBean private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test-user-id", null, List.of())
        );
    }

    @Test @DisplayName("GET /api/v1/coins/transactions - get coin history")
    void getCoinHistory() throws Exception {
        when(coinService.getUserCoinTransactions(eq("test-user-id"), any()))
                .thenReturn(List.of(CoinTransaction.builder().amount(100).build()));

        mockMvc.perform(get("/api/v1/coins/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

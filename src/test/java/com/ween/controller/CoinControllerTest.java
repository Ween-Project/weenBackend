package com.ween.controller;

import com.ween.security.SecurityUtil;
import com.ween.service.CoinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoinControllerTest {

    private CoinService coinService;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        coinService = mock(CoinService.class);
        securityUtil = mock(SecurityUtil.class);
        mockMvc = standaloneSetup(new CoinController(coinService, securityUtil))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getCoinBalanceReturnsCurrentUsersBalance() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        when(coinService.getUserCoinBalance("user-1")).thenReturn(125);

        mockMvc.perform(get("/api/v1/coins/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(125));

        verify(coinService).getUserCoinBalance("user-1");
    }

    @Test
    void getCoinTransactionsReturnsPage() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        when(coinService.getUserCoinTransactions(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/coins/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transactions retrieved successfully"));
    }
}

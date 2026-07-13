package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.AiChatRequest;
import com.ween.dto.request.AiEventSuggestRequest;
import com.ween.dto.response.AiEventSuggestResponse;
import com.ween.entity.AiChatMessage;
import com.ween.security.SecurityUtil;
import com.ween.service.AiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AiControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AiService aiService;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        aiService = mock(AiService.class);
        securityUtil = mock(SecurityUtil.class);
        mockMvc = standaloneSetup(new AiController(aiService, securityUtil))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        ControllerTestSupport.authenticateAs("user-1");
    }

    @AfterEach
    void tearDown() {
        ControllerTestSupport.clearAuthentication();
    }

    @Test
    void suggestEventContentReturnsSuggestedResponse() throws Exception {
        AiEventSuggestRequest request = new AiEventSuggestRequest("Environment Clean", "Nature", "Needs water");
        AiEventSuggestResponse serviceResponse = AiEventSuggestResponse.builder()
                .description("Suggested desc")
                .requirements(List.of("req1"))
                .schedule(List.of("sched1"))
                .build();

        when(aiService.suggestEventContent("Environment Clean", "Nature", "Needs water"))
                .thenReturn(serviceResponse);

        mockMvc.perform(post("/api/v1/ai/suggest-event-content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Content generated successfully"))
                .andExpect(jsonPath("$.data.description").value("Suggested desc"));
    }

    @Test
    void chatReturnsAiResponse() throws Exception {
        AiChatRequest request = new AiChatRequest("Hi AI");
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        when(aiService.chatWithAssistant("Hi AI", "user-1")).thenReturn("Hello User");

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Response generated successfully"))
                .andExpect(jsonPath("$.data.response").value("Hello User"));
    }

    @Test
    void getHistoryReturnsChatHistory() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        AiChatMessage msg = AiChatMessage.builder().userId("user-1").sender("AI").content("Hi").build();
        when(aiService.getChatHistory(eq("user-1"), any()))
                .thenReturn(new PageImpl<>(List.of(msg), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/api/v1/ai/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("AI chat history retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].content").value("Hi"));
    }

    @Test
    void clearHistoryDeletesHistory() throws Exception {
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");

        mockMvc.perform(delete("/api/v1/ai/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("AI chat history cleared successfully"));

        verify(aiService).clearChatHistory("user-1");
    }
}

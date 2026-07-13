package com.ween.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.response.AiEventSuggestResponse;
import com.ween.entity.AiChatMessage;
import com.ween.entity.User;
import com.ween.repository.AiChatMessageRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AiServiceTest {

    private GeminiService geminiService;
    private UserRepository userRepository;
    private AiChatMessageRepository aiChatMessageRepository;
    private ObjectMapper objectMapper;
    private AiService aiService;

    @BeforeEach
    void setUp() {
        geminiService = mock(GeminiService.class);
        userRepository = mock(UserRepository.class);
        aiChatMessageRepository = mock(AiChatMessageRepository.class);
        objectMapper = new ObjectMapper(); // Use real object mapper to test JSON parsing

        aiService = new AiService(geminiService, userRepository, aiChatMessageRepository, objectMapper);
    }

    @Test
    void suggestEventContentReturnsParsedResponseOnSuccess() {
        String jsonResult = "{\n" +
                "  \"description\": \"Təsvir\",\n" +
                "  \"requirements\": [\"Tələb 1\"],\n" +
                "  \"schedule\": [\"Cədvəl 1\"]\n" +
                "}";
        when(geminiService.generateContent(anyString(), anyString())).thenReturn(jsonResult);

        AiEventSuggestResponse response = aiService.suggestEventContent("Test Event", "Education", "No notes");

        assertThat(response).isNotNull();
        assertThat(response.getDescription()).isEqualTo("Təsvir");
        assertThat(response.getRequirements()).containsExactly("Tələb 1");
        assertThat(response.getSchedule()).containsExactly("Cədvəl 1");
    }

    @Test
    void suggestEventContentReturnsFallbackResponseOnParsingFailure() {
        when(geminiService.generateContent(anyString(), anyString())).thenReturn("invalid json");

        AiEventSuggestResponse response = aiService.suggestEventContent("Test Event", "Education", "No notes");

        assertThat(response).isNotNull();
        assertThat(response.getDescription()).contains("Test Event");
        assertThat(response.getRequirements()).isNotEmpty();
    }

    @Test
    void chatWithAssistantSavesMessagesAndReturnsResponse() {
        String userId = "user-123";
        String userMessage = "Salam, mənim neçə coinim var?";
        String aiResponse = "Salam! Sizin 100 Ween Coininiz var.";

        User user = mock(User.class);
        when(user.getFullName()).thenReturn("Ali Aliyev");
        when(user.getUsername()).thenReturn("ali123");
        when(user.getWeenCoinBalance()).thenReturn(100);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AiChatMessage historyMessage = AiChatMessage.builder()
                .userId(userId)
                .sender("USER")
                .content("Sual")
                .build();
        Page<AiChatMessage> historyPage = new PageImpl<>(List.of(historyMessage));
        when(aiChatMessageRepository.findByUserIdOrderByCreatedAtAsc(eq(userId), any(Pageable.class)))
                .thenReturn(historyPage);

        when(geminiService.generateContent(eq(userMessage), anyString())).thenReturn(aiResponse);

        String result = aiService.chatWithAssistant(userMessage, userId);

        assertThat(result).isEqualTo(aiResponse);

        ArgumentCaptor<AiChatMessage> messageCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
        verify(aiChatMessageRepository, times(2)).save(messageCaptor.capture());

        List<AiChatMessage> savedMessages = messageCaptor.getAllValues();
        assertThat(savedMessages.get(0).getSender()).isEqualTo("USER");
        assertThat(savedMessages.get(0).getContent()).isEqualTo(userMessage);
        assertThat(savedMessages.get(1).getSender()).isEqualTo("AI");
        assertThat(savedMessages.get(1).getContent()).isEqualTo(aiResponse);
    }

    @Test
    void getChatHistoryReturnsPage() {
        String userId = "user-123";
        Pageable pageable = PageRequest.of(0, 10);
        Page<AiChatMessage> expectedPage = new PageImpl<>(List.of());
        when(aiChatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId, pageable)).thenReturn(expectedPage);

        Page<AiChatMessage> result = aiService.getChatHistory(userId, pageable);

        assertThat(result).isEqualTo(expectedPage);
    }

    @Test
    void clearChatHistoryDeletesMessages() {
        String userId = "user-123";
        aiService.clearChatHistory(userId);
        verify(aiChatMessageRepository).deleteByUserId(userId);
    }
}

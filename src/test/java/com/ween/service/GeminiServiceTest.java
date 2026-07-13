package com.ween.service;

import com.ween.config.GeminiProperties;
import com.ween.dto.gemini.GeminiRequest;
import com.ween.dto.gemini.GeminiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiServiceTest {

    private RestClient geminiRestClient;
    private GeminiProperties geminiProperties;
    private GeminiService geminiService;

    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        geminiRestClient = mock(RestClient.class);
        geminiProperties = mock(GeminiProperties.class);
        geminiService = new GeminiService(geminiRestClient, geminiProperties);

        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(geminiProperties.getModel()).thenReturn("gemini-1.5-flash");
        when(geminiProperties.getApiKey()).thenReturn("test-api-key");
    }

    @Test
    void generateContentReturnsTextOnSuccess() {
        // Setup RestClient mock chain
        when(geminiRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GeminiRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        GeminiResponse response = new GeminiResponse();
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate();
        GeminiResponse.Content content = new GeminiResponse.Content();
        GeminiResponse.Part part = new GeminiResponse.Part();
        part.setText("This is an AI generated description.");
        content.setParts(List.of(part));
        candidate.setContent(content);
        response.setCandidates(List.of(candidate));

        when(responseSpec.body(GeminiResponse.class)).thenReturn(response);

        String result = geminiService.generateContent("Create description", "You are an assistant");

        assertThat(result).isEqualTo("This is an AI generated description.");
    }

    @Test
    void generateContentThrowsExceptionWhenResponseIsEmpty() {
        when(geminiRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(GeminiRequest.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        GeminiResponse response = new GeminiResponse(); // Empty candidates
        when(responseSpec.body(GeminiResponse.class)).thenReturn(response);

        assertThatThrownBy(() -> geminiService.generateContent("Create description", "You are an assistant"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI service unavailable");
    }
}

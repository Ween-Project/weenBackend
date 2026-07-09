package com.ween.service;

import com.ween.config.GeminiProperties;
import com.ween.dto.gemini.GeminiRequest;
import com.ween.dto.gemini.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final RestClient geminiRestClient;
    private final GeminiProperties geminiProperties;

    public String generateContent(String prompt, String systemInstruction) {
        try {
            GeminiRequest.Content content = GeminiRequest.Content.builder()
                    .parts(Collections.singletonList(GeminiRequest.Part.builder().text(prompt).build()))
                    .build();

            GeminiRequest.SystemInstruction sysInstruction = null;
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                sysInstruction = GeminiRequest.SystemInstruction.builder()
                        .parts(Collections.singletonList(GeminiRequest.Part.builder().text(systemInstruction).build()))
                        .build();
            }

            GeminiRequest request = GeminiRequest.builder()
                    .contents(Collections.singletonList(content))
                    .systemInstruction(sysInstruction)
                    .build();

            String uri = String.format("/v1beta/models/%s:generateContent?key=%s",
                    geminiProperties.getModel(),
                    geminiProperties.getApiKey());

            GeminiResponse response = geminiRestClient.post()
                    .uri(uri)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                GeminiResponse.Candidate candidate = response.getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null && !candidate.getContent().getParts().isEmpty()) {
                    return candidate.getContent().getParts().get(0).getText();
                }
            }
            
            throw new RuntimeException("Empty response from Gemini API");
        } catch (Exception e) {
            log.error("Failed to generate content from Gemini API", e);
            throw new RuntimeException("AI service unavailable", e);
        }
    }
}

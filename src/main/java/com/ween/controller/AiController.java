package com.ween.controller;

import com.ween.dto.request.AiChatRequest;
import com.ween.dto.request.AiEventSuggestRequest;
import com.ween.dto.response.AiChatResponse;
import com.ween.dto.response.AiEventSuggestResponse;
import com.ween.dto.response.ApiResponse;
import com.ween.security.SecurityUtil;
import com.ween.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.ween.entity.AiChatMessage;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Integration", description = "Endpoints for Google Gemini AI features")
public class AiController {

    private final AiService aiService;
    private final SecurityUtil securityUtil;

    @PostMapping("/suggest-event-content")
    @Operation(summary = "Suggest event content", description = "Automatically generates event description, requirements, and schedule using Gemini AI")
    @SecurityRequirement(name = "Bearer")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN', 'ORGANIZER')")
    public ResponseEntity<ApiResponse<AiEventSuggestResponse>> suggestEventContent(
            @Valid @RequestBody AiEventSuggestRequest request) {
        AiEventSuggestResponse response = aiService.suggestEventContent(
                request.getTitle(),
                request.getCategory(),
                request.getAdditionalNotes()
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Content generated successfully"));
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI assistant", description = "Chat with the Ween AI platform assistant about platform rules, rewards, and your account info")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request) {
        String userId = securityUtil.getCurrentUserId();
        String responseText = aiService.chatWithAssistant(request.getMessage(), userId);
        AiChatResponse response = AiChatResponse.builder().response(responseText).build();
        return ResponseEntity.ok(ApiResponse.ok(response, "Response generated successfully"));
    }

    @GetMapping("/history")
    @Operation(summary = "Get AI chat history", description = "Retrieve paginated AI chat messages for the current user")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Page<AiChatMessage>>> getHistory(
            @PageableDefault(size = 50) Pageable pageable) {
        String userId = securityUtil.getCurrentUserId();
        Page<AiChatMessage> response = aiService.getChatHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "AI chat history retrieved successfully"));
    }

    @DeleteMapping("/history")
    @Operation(summary = "Clear AI chat history", description = "Delete all AI chat messages for the current user")
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<Void>> clearHistory() {
        String userId = securityUtil.getCurrentUserId();
        aiService.clearChatHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "AI chat history cleared successfully"));
    }
}

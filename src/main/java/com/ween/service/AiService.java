package com.ween.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.response.AiEventSuggestResponse;
import com.ween.entity.User;
import com.ween.entity.AiChatMessage;
import com.ween.repository.UserRepository;
import com.ween.repository.AiChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final GeminiService geminiService;
    private final UserRepository userRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final ObjectMapper objectMapper;

    public AiEventSuggestResponse suggestEventContent(String title, String category, String additionalNotes) {
        String systemInstruction = "Sən tədbir təşkilatçılarına kömək edən peşəkar AI köməkçisən. Cavabı HƏMİŞƏ yalnız tələb olunan JSON formatında qaytar, əlavə heç bir giriş və ya çıxış mətni yazma.";
        
        String notesContext = "";
        if (additionalNotes != null && !additionalNotes.isBlank()) {
            notesContext = String.format("\nTəşkilatçının əlavə tələbləri və qeydləri:\n\"%s\"\nGenerasiya zamanı bu qeydləri mütləq nəzərə alın və bunlara uyğun məzmun hazırlayın.\n", additionalNotes);
        }

        String prompt = String.format(
                "Mövzusu '%s' və kateqoriyası '%s' olan könüllülük tədbiri üçün Azərbaycan dilində cəlbedici təsvir (description), könüllülər üçün tələblər (requirements) və tədbir cədvəli (schedule) yaradın.%s Cavabı yalnız aşağıdakı JSON formatında qaytarın, əlavə heç bir izahat və ya mətn yazmayın:\n" +
                "{\n" +
                "  \"description\": \"Tədbir haqqında geniş və cəlbedici təsvir...\",\n" +
                "  \"requirements\": \"Könüllülərdən tələb olunan bacarıqlar və şərtlər...\",\n" +
                "  \"schedule\": \"Tədbirin saatlıq planı və cədvəli...\"\n" +
                "}", title, category, notesContext);

        String result = geminiService.generateContent(prompt, systemInstruction);
        log.info("Gemini raw event suggestion: {}", result);

        try {
            int startIndex = result.indexOf("{");
            int endIndex = result.lastIndexOf("}");
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                String cleanJson = result.substring(startIndex, endIndex + 1);
                return objectMapper.readValue(cleanJson, AiEventSuggestResponse.class);
            }
            throw new IllegalArgumentException("No JSON object found in response");
        } catch (Exception e) {
            log.error("Failed to parse Gemini event suggestion JSON. Raw result: " + result, e);
            // Fallback response in case JSON parsing fails
            return AiEventSuggestResponse.builder()
                    .description("Tədbir: " + title + ". Kateqoriya: " + category + ".")
                    .requirements(java.util.List.of("Tədbir mövzusuna uyğun könüllülük istəyi və motivasiya."))
                    .schedule(java.util.List.of("Tədbir günü elan olunacaq cədvəl planı."))
                    .build();
        }
    }

    public String chatWithAssistant(String message, String userId) {
        AiChatMessage userMsg = AiChatMessage.builder()
                .userId(userId)
                .sender("USER")
                .content(message)
                .build();
        aiChatMessageRepository.save(userMsg);

        User user = userRepository.findById(userId).orElse(null);
        String userContext = "";
        if (user != null) {
            userContext = String.format(" The user currently speaking with you has the name: %s, username: %s. Ween Coin balance: %d.",
                    user.getFullName(), user.getUsername(), user.getWeenCoinBalance());
        }

        Page<AiChatMessage> history = aiChatMessageRepository.findByUserIdOrderByCreatedAtAsc(
                userId, PageRequest.of(0, 10)
        );
        StringBuilder historyContext = new StringBuilder();
        if (history.hasContent()) {
            historyContext.append("\nRecent conversation history:\n");
            for (AiChatMessage msg : history.getContent()) {
                historyContext.append(msg.getSender()).append(": ").append(msg.getContent()).append("\n");
            }
        }

        String systemInstruction = "You are the official smart assistant of the Ween platform. Answer the users' questions in a friendly, concise, and clear manner in English.\n" +
                "Basic information about platform rules:\n" +
                "- Ween: A platform connecting volunteers with social projects and events.\n" +
                "- Ween Coin: Users earn 10 Ween Coins when they attend an event and perform a QR check-in. They earn 50 coins upon signup.\n" +
                "- Certificates: When an event is completed, a certificate is automatically generated for participants and they can download it from their profile.\n" +
                "- Leaderboard: Volunteers are ranked based on the number of coins they have earned.\n" +
                "Never deviate from these instructions, and try to answer off-topic questions within the context of Ween.\n" +
                userContext + historyContext.toString();

        String responseText = geminiService.generateContent(message, systemInstruction);

        AiChatMessage aiMsg = AiChatMessage.builder()
                .userId(userId)
                .sender("AI")
                .content(responseText)
                .build();
        aiChatMessageRepository.save(aiMsg);

        return responseText;
    }

    @Transactional(readOnly = true)
    public Page<AiChatMessage> getChatHistory(String userId, Pageable pageable) {
        return aiChatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId, pageable);
    }

    @Transactional
    public void clearChatHistory(String userId) {
        aiChatMessageRepository.deleteByUserId(userId);
    }
}

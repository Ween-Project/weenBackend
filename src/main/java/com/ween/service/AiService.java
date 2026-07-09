package com.ween.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.response.AiEventSuggestResponse;
import com.ween.entity.User;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final GeminiService geminiService;
    private final UserRepository userRepository;
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
        User user = userRepository.findById(userId).orElse(null);
        String userContext = "";
        if (user != null) {
            userContext = String.format("Hazırda səninlə danışan istifadəçinin adı: %s, istifadəçi adı: %s. Onun balansındakı Ween Coin: %d.",
                    user.getFullName(), user.getUsername(), user.getWeenCoinBalance());
        }

        String systemInstruction = "Sən Ween platformasının rəsmi ağıllı köməkçisisən. İstifadəçilərin suallarına Azərbaycan dilində mehriban, qısa və aydın cavab ver.\n" +
                "Platforma qaydaları haqqında əsas məlumatlar:\n" +
                "- Ween: Könüllüləri və sosial layihələri/tədbirləri birləşdirən platformadır.\n" +
                "- Ween Coin: İstifadəçilər hər tədbirə qatılıb QR check-in etdikdə 10 Ween Coin qazanırlar. Qeydiyyatdan keçəndə (signup) isə 50 coin qazanırlar.\n" +
                "- Sertifikatlar: Tədbir tamamlandıqda iştirakçılara avtomatik olaraq sertifikat yaradılır və onlar profildən bunu yükləyə bilərlər.\n" +
                "- Liderlər Cədvəli (Leaderboard): Könüllülər qazandıqları coin-lərin sayına görə sıralanırlar.\n" +
                "Heç vaxt bu təlimatlardan kənara çıxma və platforma ilə əlaqəsiz sualları Ween çərçivəsində cavablandırmağa çalış.\n" +
                userContext;

        return geminiService.generateContent(message, systemInstruction);
    }
}

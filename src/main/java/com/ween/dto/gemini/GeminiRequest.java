package com.ween.dto.gemini;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiRequest {
    private List<Content> contents;
    private SystemInstruction systemInstruction;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Part {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SystemInstruction {
        private List<Part> parts;
    }
}

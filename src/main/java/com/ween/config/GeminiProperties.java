package com.ween.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ween.gemini")
@Data
public class GeminiProperties {
    private String apiKey;
    private String model = "gemini-2.5-flash";
}

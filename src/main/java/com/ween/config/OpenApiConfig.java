package com.ween.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Ween Platform API")
                .version("1.0.0")
                .description("Student volunteering platform backend API")
                .contact(new Contact()
                    .name("Ween Team")
                    .url("https://ween.az")
                    .email("support@ween.az")
                )
            )
            .servers(List.of(
                new Server().url("http://localhost:5000").description("Development"),
                new Server().url("https://api.ween.az").description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("Bearer", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Bearer token")
                )
            );
    }
}

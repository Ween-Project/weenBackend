package com.ween.config;

import com.ween.security.ApiKeyFilter;
import com.ween.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyFilter apiKeyFilter;
    private final UserDetailsService userDetailsService;

    @Value("${ween.cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://localhost:5000,http://localhost:5001,http://localhost:5173,http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return builder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints - public register/login/refresh, secured logout
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/organization").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login/organization").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/change-password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

                        // Event registration
                        .requestMatchers(HttpMethod.POST, "/api/v1/events/*/register").hasRole("VOLUNTEER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/events/*/register").hasRole("VOLUNTEER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/participants").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*/stats").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN")

                        // Events - public read, requires auth for write
                        .requestMatchers(HttpMethod.GET, "/api/v1/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/events/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/events").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/events/**").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/events/**").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN", "ADMIN")

                        // QR check-in - authenticated organizer flow
                        .requestMatchers(HttpMethod.POST, "/api/v1/qr/checkin").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN")

                        // Certificates - public verify, requires auth for download
                        .requestMatchers(HttpMethod.GET, "/api/v1/certificates/verify/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/certificates/generate/**").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/certificates/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/certificates/my").authenticated()

                        // Coins & Leaderboard - requires auth
                        .requestMatchers("/api/v1/coins/**").authenticated()

                        // Users
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/@*").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/me/profile-photo").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me/**").authenticated()

                        // Organizations
                        .requestMatchers(HttpMethod.GET, "/api/v1/organizations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/organizations").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/organizations/**").hasAnyRole("ORGANIZER", "ORGANIZATION_ADMIN")

                        // Notifications
                        .requestMatchers("/api/v1/notifications/**").authenticated()

                        // Chat
                        .requestMatchers("/api/v1/chat/**").authenticated()

                        // Admin
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Swagger
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()

                        // WebSockets
                        .requestMatchers("/ws/**").permitAll()

                        // Health check
                        .requestMatchers("/actuator/**").permitAll()

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(401, "Unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendError(403, "Forbidden");
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> configuredOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .collect(Collectors.toList());

        // Keep exact origins and wildcard patterns separate for proper credential support.
        List<String> exactOrigins = configuredOrigins.stream()
                .filter(origin -> !origin.contains("*"))
                .collect(Collectors.toList());
        List<String> originPatterns = configuredOrigins.stream()
                .filter(origin -> origin.contains("*"))
                .collect(Collectors.toList());

        configuration.setAllowedOrigins(exactOrigins);
        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

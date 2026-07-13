package com.ween.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${ween.api-key}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (requestPath.contains("/api/v1/participations/checkin-join")) {
            String apiKey = request.getHeader("X-Api-Key");
            String authHeader = request.getHeader("Authorization");

            boolean hasBearer = authHeader != null && authHeader.startsWith("Bearer ");
            boolean hasValidApiKey = apiKey != null && apiKey.equals(validApiKey);

            if (hasBearer) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!hasValidApiKey) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Invalid API Key or Bearer Token\"}");
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "api-client",
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("API Key authentication successful");
        }

        filterChain.doFilter(request, response);
    }
}

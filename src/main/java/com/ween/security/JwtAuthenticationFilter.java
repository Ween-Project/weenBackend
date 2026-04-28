package com.ween.security;

import com.ween.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                log.info("JWT token found in Authorization header");

                boolean isValid = jwtUtil.validateToken(token);
                boolean isNotExpired = !jwtUtil.isTokenExpired(token);
                log.info("Token validation: valid={}, notExpired={}", isValid, isNotExpired);

                if (isValid && isNotExpired) {
                    String tokenType = jwtUtil.extractTokenType(token);
                    log.info("Token type: {}", tokenType);

                    if (!"access".equals(tokenType)) {
                        log.warn("Token type is not 'access', skipping authentication");
                        filterChain.doFilter(request, response);
                        return;
                    }

                    String userId = jwtUtil.extractUserId(token);
                    UserRole role = jwtUtil.extractRole(token);
                    log.info("Extracted userId: {}, role: {}", userId, role);

                    if (role != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userId,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("JWT token validated and authenticated for user: {} with role: {}", userId, role);
                    } else {
                        log.warn("Role is null, authentication not set");
                    }
                } else {
                    log.warn("Token validation failed or token expired");
                }
            } else {
                log.debug("No Bearer token in Authorization header");
            }
        } catch (Exception ex) {
            log.error("JWT filter error", ex);
        }

        filterChain.doFilter(request, response);
    }
}

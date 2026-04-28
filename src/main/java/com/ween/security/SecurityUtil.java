package com.ween.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityUtil {

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User not authenticated");
            throw new RuntimeException("User not authenticated");
        }

        String userId = (String) authentication.getPrincipal();
        log.debug("Current user ID extracted: {}", userId);
        return userId;
    }
}

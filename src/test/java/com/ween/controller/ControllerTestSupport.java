package com.ween.controller;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;

final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static void authenticateAs(String userId) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(userId, null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    static Principal principal(String userId) {
        return () -> userId;
    }
}

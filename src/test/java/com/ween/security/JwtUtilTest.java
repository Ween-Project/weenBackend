package com.ween.security;

import com.ween.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set properties required for JWT signing
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "test-secret-key-that-is-at-least-32-characters-long-for-hmac");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiry", 900L); // 15 mins
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiry", 604800L); // 7 days
    }

    @Test @DisplayName("Generate access token - contains correct claims")
    void generateAccessToken() {
        String token = jwtUtil.generateAccessToken("uid", "u@e.com", UserRole.VOLUNTEER);
        assertThat(token).isNotBlank();
        
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("uid");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("u@e.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo(UserRole.VOLUNTEER);
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("access");
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test @DisplayName("Generate refresh token - contains correct claims")
    void generateRefreshToken() {
        String token = jwtUtil.generateRefreshToken("uid");
        assertThat(token).isNotBlank();
        
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("uid");
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("refresh");
        assertThat(jwtUtil.extractEmail(token)).isNull(); // refresh shouldn't have email
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test @DisplayName("Validate token - tampered signature returns false")
    void validateToken_tampered() {
        String token = jwtUtil.generateAccessToken("uid", "u@e.com", UserRole.VOLUNTEER);
        String tampered = token.substring(0, token.length() - 5) + "abcde";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }
}

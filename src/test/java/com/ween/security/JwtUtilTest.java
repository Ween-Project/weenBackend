package com.ween.security;

import com.ween.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiry", 900L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiry", 604800L);
    }

    @Test
    void accessTokenContainsExpectedClaims() {
        String token = jwtUtil.generateAccessToken("user-1", "ali@example.com", UserRole.VOLUNTEER);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-1");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("ali@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo(UserRole.VOLUNTEER);
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("access");
    }

    @Test
    void refreshTokenContainsRefreshTypeOnly() {
        String token = jwtUtil.generateRefreshToken("user-1");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-1");
        assertThat(jwtUtil.extractRole(token)).isNull();
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("refresh");
    }
}

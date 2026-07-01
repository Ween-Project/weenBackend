package com.ween.security;

import com.ween.entity.User;
import com.ween.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    @Value("${ween.jwt.secret}")
    private String jwtSecret;

    @Value("${ween.jwt.access-token-expiry:900}")
    private long accessTokenExpiry;

    @Value("${ween.jwt.refresh-token-expiry:604800}")
    private long refreshTokenExpiry;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(String userId, String email,UserRole role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("email", email);
        claims.put("type", "access");
        return createToken(claims, userId, accessTokenExpiry);
    }

    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, userId, refreshTokenExpiry);
    }

    public String generateQrToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "qr");
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole());
        if (user.getUniversity() != null) claims.put("university", user.getUniversity());
        if (user.getMajor() != null) claims.put("major", user.getMajor());

        // 30 seconds expiry for QR code
        return createToken(claims, user.getId(), 30);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiry) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry * 1000))
                .id(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    public String extractEmail(String token) {
        return (String) extractClaims(token).get("email");
    }

    public UserRole extractRole(String token) {
        Object roleClaim = extractClaims(token).get("role");
        if (roleClaim == null) {
            return null;
        }
        return UserRole.valueOf(roleClaim.toString());
    }

    public String extractTokenType(String token) {
        Object typeClaim = extractClaims(token).get("type");
        return typeClaim == null ? null : typeClaim.toString();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            log.debug("JWT token validation successful");
            return true;
        } catch (Exception ex) {
            log.error("JWT validation failed: {} (class: {})", ex.getMessage(), ex.getClass().getSimpleName());
            return false;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiry * 1000; // Convert to milliseconds
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception ex) {
            return true;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package com.ween.config;

import com.ween.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String token = resolveToken(request);

        if (token == null || !jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (!"access".equals(jwtUtil.extractTokenType(token))) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String userId = jwtUtil.extractUserId(token);
        attributes.put("userId", userId);
        log.debug("WebSocket handshake authenticated for user: {}", userId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }

    private String resolveToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders != null) {
            for (String header : authHeaders) {
                if (header != null && header.startsWith("Bearer ")) {
                    return header.substring(7);
                }
            }
        }

        var params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = params.getFirst("token");
        return token != null ? token : params.getFirst("access_token");
    }
}

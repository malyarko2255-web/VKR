package ru.finuniversity.advance.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.finuniversity.advance.common.dto.ApiError;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidationGlobalFilter implements GlobalFilter, Ordered {

    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/actuator", "/api/v1/auth", "/fallback"
    );

    private final ReactiveJwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (SKIP_PREFIXES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    String userId   = jwt.getSubject();
                    String username = jwt.getClaimAsString("preferred_username");
                    String role     = extractRole(jwt);

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id",   userId   != null ? userId   : "")
                            .header("X-User-Role", role     != null ? role     : "")
                            .header("X-Username",  username != null ? username : "")
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .onErrorResume(ex -> {
                    log.debug("JWT validation failed: {}", ex.getMessage());
                    return unauthorized(exchange, "Invalid or expired token");
                });
    }

    private String extractRole(org.springframework.security.oauth2.jwt.Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
        ApiError error = new ApiError(Instant.now(), 401, message,
                exchange.getRequest().getPath().value(), requestId);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(error);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":401,\"message\":\"" + message + "\"}").getBytes();
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}

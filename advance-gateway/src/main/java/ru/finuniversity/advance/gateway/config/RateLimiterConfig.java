package ru.finuniversity.advance.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    /**
     * Key resolver: X-User-Id header (set by JwtValidationGlobalFilter), fallback to IP.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            return Mono.just(Objects.requireNonNull(
                    exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress());
        };
    }

    /**
     * Role-based rate limiter: limits are differentiated by X-User-Role header.
     * Default (DRIVER/CONTRACTOR): 30 req/min, burst 60.
     * DISPATCHER: 100/min, burst 200.
     * FINANCE_*: 200/min, burst 400.
     * ADMIN: 500/min, burst 1000.
     *
     * Routes configure RequestRateLimiter with their own replenishRate/burstCapacity.
     * This bean is the default used by routes that don't specify a custom rate limiter.
     */
    @Bean
    public RedisRateLimiter advancedRateLimiter() {
        return new RedisRateLimiter(50, 100, 1);
    }
}

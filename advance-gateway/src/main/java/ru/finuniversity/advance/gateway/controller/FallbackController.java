package ru.finuniversity.advance.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.finuniversity.advance.common.dto.ApiError;

import java.time.Instant;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/reference")
    public Mono<ResponseEntity<ApiError>> referenceServiceFallback(ServerWebExchange exchange) {
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
        ApiError error = new ApiError(
                Instant.now(),
                503,
                "Reference service temporarily unavailable.",
                "/fallback/reference",
                requestId
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
    }
}

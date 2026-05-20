package ru.finuniversity.advance.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Slf4j
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method    = exchange.getRequest().getMethod().name();
        String path      = exchange.getRequest().getPath().value();
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-ID");
        String ip        = resolveIp(exchange);

        log.info("[REQUEST] {} {} from {} requestId={}", method, path, ip, requestId);

        long start = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;
            long elapsed = System.currentTimeMillis() - start;
            log.info("[RESPONSE] {} {} → {} in {}ms requestId={}", method, path, status, elapsed, requestId);
        }));
    }

    private String resolveIp(ServerWebExchange exchange) {
        InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }
}

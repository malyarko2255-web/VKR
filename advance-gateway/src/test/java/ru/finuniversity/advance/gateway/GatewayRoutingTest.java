package ru.finuniversity.advance.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
        "spring.cloud.gateway.httpclient.pool.type=DISABLED"
    }
)
@AutoConfigureWebTestClient(timeout = "PT30S")
class GatewayRoutingTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    ReactiveJwtDecoder reactiveJwtDecoder;

    @MockBean
    RedisRateLimiter redisRateLimiter;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("services.core.url", wireMockServer::baseUrl);
        registry.add("services.identity.url", wireMockServer::baseUrl);
        registry.add("services.payment.url", wireMockServer::baseUrl);
        registry.add("services.scoring.url", wireMockServer::baseUrl);
        registry.add("services.reference.url", wireMockServer::baseUrl);
        registry.add("services.notification.url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void setupRateLimiter() {
        Map<String, String> headers = new HashMap<>();
        when(redisRateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(new RateLimiter.Response(true, headers)));
    }

    @Test
    void advanceEndpoint_withValidJwt_routedToCore() {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/advances"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Connection", "close")
                        .withBody("[]")));

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("user-123")
                .claim("preferred_username", "testuser")
                .claim("roles", List.of("DRIVER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(reactiveJwtDecoder.decode("test-token")).thenReturn(Mono.just(jwt));

        webTestClient.get()
                .uri("/api/v1/advances")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        webTestClient.get()
                .uri("/api/v1/advances")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rateLimiter_exceededLimit_returns429() {
        AtomicInteger callCount = new AtomicInteger(0);
        Map<String, String> headers = new HashMap<>();

        when(redisRateLimiter.isAllowed(anyString(), anyString()))
                .thenAnswer(inv -> {
                    int count = callCount.incrementAndGet();
                    boolean allowed = count <= 5;
                    return Mono.just(new RateLimiter.Response(allowed, headers));
                });

        Jwt jwt = Jwt.withTokenValue("rate-token")
                .header("alg", "RS256")
                .subject("user-rate")
                .claim("preferred_username", "rateuser")
                .claim("roles", List.of("DRIVER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(reactiveJwtDecoder.decode("rate-token")).thenReturn(Mono.just(jwt));

        wireMockServer.stubFor(get(urlPathEqualTo("/api/v1/advances"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Connection", "close")
                        .withBody("[]")));

        for (int i = 0; i < 5; i++) {
            webTestClient.get()
                    .uri("/api/v1/advances")
                    .header("Authorization", "Bearer rate-token")
                    .exchange()
                    .expectStatus().isOk();
        }

        webTestClient.get()
                .uri("/api/v1/advances")
                .header("Authorization", "Bearer rate-token")
                .exchange()
                .expectStatus().isEqualTo(429);
    }
}

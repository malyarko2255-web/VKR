package ru.finuniversity.advance.core;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.dto.AdvanceType;
import ru.finuniversity.advance.common.util.KafkaTopics;
import ru.finuniversity.advance.core.client.ReferenceClient;
import ru.finuniversity.advance.core.dto.DriverResponseDto;
import ru.finuniversity.advance.core.dto.LimitResponseDto;
import ru.finuniversity.advance.core.entity.Advance;
import ru.finuniversity.advance.core.repository.AdvanceHistoryRepository;
import ru.finuniversity.advance.core.repository.AdvanceRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class AdvanceIntegrationTest {

    // ── Containers ──────────────────────────────────────────────────────────

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("advance_core")
                    .withUsername("advance_user")
                    .withPassword("advance_pass");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // Disable OAuth JWK auto-fetch
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:9999/realms/test/protocol/openid-connect/certs");
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    static final UUID DRIVER_1      = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID DRIVER_2      = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID ROUTE_ID      = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID DISPATCHER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    static final String DRIVER1_TOKEN     = "driver1-token";
    static final String DRIVER2_TOKEN     = "driver2-token";
    static final String DISPATCHER_TOKEN  = "dispatcher-token";

    // ── Spring beans ─────────────────────────────────────────────────────────

    @Autowired TestRestTemplate          restTemplate;
    @Autowired AdvanceRepository         advanceRepository;
    @Autowired AdvanceHistoryRepository  historyRepository;
    @Autowired StringRedisTemplate       redisTemplate;

    @MockBean ReferenceClient referenceClient;
    @MockBean JwtDecoder      jwtDecoder;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setup() {
        historyRepository.deleteAll();
        advanceRepository.deleteAll();
        flushRedis();

        DriverResponseDto d1 = new DriverResponseDto(DRIVER_1, UUID.randomUUID(),
                "Иван Иванов", "AA123456", "+79001234567", null, null, true);
        DriverResponseDto d2 = new DriverResponseDto(DRIVER_2, UUID.randomUUID(),
                "Петр Петров", "BB654321", "+79009876543", null, null, true);

        when(referenceClient.getDriver(DRIVER_1)).thenReturn(d1);
        when(referenceClient.getDriver(DRIVER_2)).thenReturn(d2);
        when(referenceClient.getDriver(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            if (DRIVER_2.equals(id)) return d2;
            return d1;
        });
        when(referenceClient.getDriverTrips(any())).thenReturn(Collections.emptyList());
        when(referenceClient.getDriverLimits(any())).thenReturn(List.of(
                new LimitResponseDto(UUID.randomUUID(), "FUEL",
                        BigDecimal.valueOf(50_000), BigDecimal.valueOf(5_000),
                        BigDecimal.valueOf(5_000))
        ));

        when(jwtDecoder.decode(DRIVER1_TOKEN)).thenReturn(
                buildJwt(DRIVER_1.toString(), "DRIVER"));
        when(jwtDecoder.decode(DRIVER2_TOKEN)).thenReturn(
                buildJwt(DRIVER_2.toString(), "DRIVER"));
        when(jwtDecoder.decode(DISPATCHER_TOKEN)).thenReturn(
                buildJwt(DISPATCHER_ID.toString(), "DISPATCHER"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Jwt buildJwt(String subject, String role) {
        return Jwt.withTokenValue("tok")
                .header("alg", "RS256")
                .subject(subject)
                .claim("roles", List.of(role))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private void setScore(UUID driverId, int score) {
        redisTemplate.opsForValue().set("score:" + driverId, String.valueOf(score));
    }

    private void setLimitConfig(UUID driverId, AdvanceType type,
                                BigDecimal monthly, BigDecimal threshold) {
        String key = "limit:" + driverId + ":" + type.name();
        redisTemplate.opsForHash().put(key, "monthly",   monthly.toPlainString());
        redisTemplate.opsForHash().put(key, "daily",     "0");
        redisTemplate.opsForHash().put(key, "auto_threshold", threshold.toPlainString());
    }

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpEntity<String> req(String token, String body) {
        return new HttpEntity<>(body, headers(token));
    }

    private String createBody(UUID driverId, AdvanceType type, BigDecimal amount) {
        return """
                {"driverId":"%s","routeId":"%s","tripStage":"IN_TRANSIT",
                 "advanceType":"%s","amount":%s}
                """.formatted(driverId, ROUTE_ID, type.name(), amount.toPlainString());
    }

    private Advance saveAdvance(UUID driverId, AdvanceStatus status, BigDecimal amount) {
        return advanceRepository.saveAndFlush(Advance.builder()
                .driverId(driverId)
                .routeId(ROUTE_ID)
                .tripStage("IN_TRANSIT")
                .advanceType(AdvanceType.FUEL)
                .amount(amount)
                .status(status)
                .limitAvailable(BigDecimal.valueOf(50_000).subtract(amount))
                .autoApproved(false)
                .scoreAtCreation(50)
                .build());
    }

    private Consumer<String, String> kafkaConsumer(String topic) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(), "test-" + UUID.randomUUID(), "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        Consumer<String, String> c = new DefaultKafkaConsumerFactory<String, String>(props)
                .createConsumer();
        c.subscribe(Collections.singletonList(topic));
        c.poll(Duration.ofMillis(200)); // warm up / establish offset
        return c;
    }

    @SuppressWarnings("deprecation")
    private void flushRedis() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory())
                .getConnection().flushDb();
    }

    // ── Scenario 1 ────────────────────────────────────────────────────────────
    // score=85, amount=4000 ≤ threshold=10000 → autoApproved=true, status=APPROVED
    // AdvanceCreatedEvent appears in Kafka

    @Test
    void scenario1_create_autoApproved_highScoreDriver() {
        setScore(DRIVER_1, 85);
        setLimitConfig(DRIVER_1, AdvanceType.FUEL,
                BigDecimal.valueOf(50_000), BigDecimal.valueOf(10_000));

        Consumer<String, String> consumer = kafkaConsumer(KafkaTopics.ADVANCES_CREATED);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/advances", HttpMethod.POST,
                req(DRIVER1_TOKEN, createBody(DRIVER_1, AdvanceType.FUEL, BigDecimal.valueOf(4_000))),
                Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().get("autoApproved")).isEqualTo(true);
        assertThat(res.getBody().get("status")).isEqualTo("APPROVED");

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.ADVANCES_CREATED, Duration.ofSeconds(10));
        assertThat(record).isNotNull();
        assertThat(record.value()).contains("ADVANCE_CREATED");
        consumer.close();
    }

    // ── Scenario 2 ────────────────────────────────────────────────────────────
    // score=45 → DISPATCHER_REVIEW, autoApproved=false

    @Test
    void scenario2_create_requiresApproval_lowScoreDriver() {
        setScore(DRIVER_1, 45);
        setLimitConfig(DRIVER_1, AdvanceType.FUEL,
                BigDecimal.valueOf(50_000), BigDecimal.valueOf(10_000));

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/advances", HttpMethod.POST,
                req(DRIVER1_TOKEN, createBody(DRIVER_1, AdvanceType.FUEL, BigDecimal.valueOf(6_000))),
                Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().get("status")).isEqualTo("DISPATCHER_REVIEW");
        assertThat(res.getBody().get("autoApproved")).isEqualTo(false);
    }

    // ── Scenario 3 ────────────────────────────────────────────────────────────
    // Monthly limit = 10000, PAID advances = 8000 → available=2000 < 5000 → 409 LIMIT_EXCEEDED

    @Test
    void scenario3_create_limitExceeded_returns409() {
        setScore(DRIVER_1, 85);
        setLimitConfig(DRIVER_1, AdvanceType.FUEL,
                BigDecimal.valueOf(10_000), BigDecimal.valueOf(5_000));
        // PAID advances consume limit but not open-advance check
        saveAdvance(DRIVER_1, AdvanceStatus.PAID, BigDecimal.valueOf(8_000));

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/advances", HttpMethod.POST,
                req(DRIVER1_TOKEN, createBody(DRIVER_1, AdvanceType.FUEL, BigDecimal.valueOf(5_000))),
                Map.class);

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().get("message").toString()).containsIgnoringCase("лимит");
    }

    // ── Scenario 4 ────────────────────────────────────────────────────────────
    // Open advance in DISPATCHER_REVIEW → 409 OPEN_ADVANCE_EXISTS

    @Test
    void scenario4_create_openAdvanceExists_returns409() {
        setScore(DRIVER_1, 85);
        setLimitConfig(DRIVER_1, AdvanceType.FUEL,
                BigDecimal.valueOf(50_000), BigDecimal.valueOf(10_000));
        saveAdvance(DRIVER_1, AdvanceStatus.DISPATCHER_REVIEW, BigDecimal.valueOf(3_000));

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/advances", HttpMethod.POST,
                req(DRIVER1_TOKEN, createBody(DRIVER_1, AdvanceType.FUEL, BigDecimal.valueOf(1_000))),
                Map.class);

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody().get("message").toString()).containsIgnoringCase("открытая заявка");
    }

    // ── Scenario 5 ────────────────────────────────────────────────────────────
    // Dispatcher approves, amount=3000 ≤ financeThreshold=5000 → APPROVED + PaymentInitiatedEvent

    @Test
    void scenario5_approve_dispatcherApproves_belowThreshold() {
        Advance advance = saveAdvance(DRIVER_1, AdvanceStatus.DISPATCHER_REVIEW, BigDecimal.valueOf(3_000));
        // getDriverLimits returns autoApproveThreshold=5000 (used as finance threshold)

        Consumer<String, String> consumer = kafkaConsumer(KafkaTopics.PAYMENTS_COMMANDS);

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/advances/" + advance.getId() + "/approve", HttpMethod.PUT,
                req(DISPATCHER_TOKEN, "{\"comment\":\"OK\"}"),
                Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("status")).isEqualTo("APPROVED");

        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, KafkaTopics.PAYMENTS_COMMANDS, Duration.ofSeconds(10));
        assertThat(record).isNotNull();
        assertThat(record.value()).contains("PAYMENT_INITIATED");
        consumer.close();
    }

    // ── Scenario 6 ────────────────────────────────────────────────────────────
    // Dispatcher approves, amount=20000 > financeThreshold=5000 → FINANCE_REVIEW

    @Test
    void scenario6_approve_dispatcherApproves_aboveThreshold() {
        Advance advance = saveAdvance(DRIVER_1, AdvanceStatus.DISPATCHER_REVIEW, BigDecimal.valueOf(20_000));

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/advances/" + advance.getId() + "/approve", HttpMethod.PUT,
                req(DISPATCHER_TOKEN, "{\"comment\":\"Needs finance review\"}"),
                Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("status")).isEqualTo("FINANCE_REVIEW");
    }

    // ── Scenario 7 ────────────────────────────────────────────────────────────
    // DRIVER role tries to approve → @PreAuthorize → 403

    @Test
    void scenario7_approve_wrongRole_returns403() {
        Advance advance = saveAdvance(DRIVER_1, AdvanceStatus.DISPATCHER_REVIEW, BigDecimal.valueOf(3_000));

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/advances/" + advance.getId() + "/approve", HttpMethod.PUT,
                req(DRIVER1_TOKEN, "{\"comment\":\"Trying to approve\"}"),
                Map.class);

        assertThat(res.getStatusCode().value()).isEqualTo(403);
    }

    // ── Scenario 8 ────────────────────────────────────────────────────────────
    // Create advance (applyLimit increments Redis key),
    // reject it (releaseLimit decrements), verify Redis limit_used returns to 0

    @Test
    void scenario8_reject_releasesRedisLimit() {
        setScore(DRIVER_1, 50);
        setLimitConfig(DRIVER_1, AdvanceType.FUEL,
                BigDecimal.valueOf(50_000), BigDecimal.valueOf(5_000));

        // Create via API (triggers applyLimit)
        ResponseEntity<Map> createRes = restTemplate.exchange(
                "/api/v1/advances", HttpMethod.POST,
                req(DRIVER1_TOKEN, createBody(DRIVER_1, AdvanceType.FUEL, BigDecimal.valueOf(4_000))),
                Map.class);
        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String advanceId = (String) createRes.getBody().get("id");
        String limitUsedKey = "limit_used:" + DRIVER_1 + ":" + AdvanceType.FUEL.name();

        String usedAfterCreate = redisTemplate.opsForValue().get(limitUsedKey);
        assertThat(usedAfterCreate).isNotNull();
        assertThat(new BigDecimal(usedAfterCreate)).isGreaterThan(BigDecimal.ZERO);

        // Reject via DISPATCHER
        when(jwtDecoder.decode(DISPATCHER_TOKEN)).thenReturn(
                buildJwt(DISPATCHER_ID.toString(), "DISPATCHER"));
        ResponseEntity<Map> rejectRes = restTemplate.exchange(
                "/api/v1/advances/" + advanceId + "/reject", HttpMethod.PUT,
                req(DISPATCHER_TOKEN, "{\"reason\":\"Test rejection\"}"),
                Map.class);
        assertThat(rejectRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejectRes.getBody().get("status")).isEqualTo("REJECTED");

        // After rejection the limit_used key should be back to ≈ 0
        String usedAfterReject = redisTemplate.opsForValue().get(limitUsedKey);
        if (usedAfterReject != null) {
            assertThat(new BigDecimal(usedAfterReject).abs())
                    .isLessThan(BigDecimal.valueOf(1)); // floating-point tolerance
        }
    }

    // ── Scenario 9 ────────────────────────────────────────────────────────────
    // DRIVER sees only own advances (role-based filter)

    @Test
    void scenario9_getList_driverSeesOnlyOwn() {
        // 3 advances for DRIVER_1
        saveAdvance(DRIVER_1, AdvanceStatus.APPROVED, BigDecimal.valueOf(1_000));
        saveAdvance(DRIVER_1, AdvanceStatus.PAID,     BigDecimal.valueOf(2_000));
        saveAdvance(DRIVER_1, AdvanceStatus.REJECTED, BigDecimal.valueOf(3_000));
        // 2 advances for DRIVER_2
        saveAdvance(DRIVER_2, AdvanceStatus.APPROVED, BigDecimal.valueOf(1_500));
        saveAdvance(DRIVER_2, AdvanceStatus.REJECTED, BigDecimal.valueOf(2_500));

        ResponseEntity<Map> res = restTemplate.exchange(
                "/api/v1/advances", HttpMethod.GET,
                new HttpEntity<>(headers(DRIVER1_TOKEN)),
                Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Spring Data Page serializes totalElements as long/integer
        Object totalElements = res.getBody().get("totalElements");
        assertThat(((Number) totalElements).intValue()).isEqualTo(3);
    }

    // ── Scenario 10 ───────────────────────────────────────────────────────────
    // Two concurrent approve requests on the same advance:
    // one gets 200, other gets 409 (optimistic lock) or 403 (status already changed)

    @Test
    void scenario10_optimisticLock_concurrentApprove_oneFails() throws Exception {
        Advance advance = saveAdvance(DRIVER_1, AdvanceStatus.DISPATCHER_REVIEW, BigDecimal.valueOf(3_000));
        String url = "/api/v1/advances/" + advance.getId() + "/approve";

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        HttpEntity<String> approveReq = req(DISPATCHER_TOKEN, "{\"comment\":\"concurrent\"}");

        Future<ResponseEntity<Map>> f1 = executor.submit(() -> {
            startLatch.await();
            return restTemplate.exchange(url, HttpMethod.PUT, approveReq, Map.class);
        });
        Future<ResponseEntity<Map>> f2 = executor.submit(() -> {
            startLatch.await();
            return restTemplate.exchange(url, HttpMethod.PUT, approveReq, Map.class);
        });

        startLatch.countDown();

        ResponseEntity<Map> r1 = f1.get(15, TimeUnit.SECONDS);
        ResponseEntity<Map> r2 = f2.get(15, TimeUnit.SECONDS);
        executor.shutdown();

        List<Integer> statuses = List.of(
                r1.getStatusCode().value(),
                r2.getStatusCode().value());

        long successes = statuses.stream().filter(s -> s == 200).count();
        long failures  = statuses.stream().filter(s -> s == 409 || s == 403).count();

        assertThat(successes).as("exactly one approve succeeds").isEqualTo(1);
        assertThat(failures).as("exactly one approve fails").isEqualTo(1);
    }
}

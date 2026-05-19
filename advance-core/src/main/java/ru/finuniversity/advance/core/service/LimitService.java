package ru.finuniversity.advance.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.dto.AdvanceType;
import ru.finuniversity.advance.common.events.LimitsChangedEvent;
import ru.finuniversity.advance.common.exception.LimitExceededException;
import ru.finuniversity.advance.core.client.ReferenceClient;
import ru.finuniversity.advance.core.dto.AvailableLimitDto;
import ru.finuniversity.advance.core.dto.LimitResponseDto;
import ru.finuniversity.advance.core.repository.AdvanceRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitService {

    private static final String LIMIT_KEY_PREFIX      = "limit:";
    private static final String LIMIT_USED_KEY_PREFIX = "limit_used:";
    private static final Duration LIMIT_TTL           = Duration.ofMinutes(5);

    private static final List<AdvanceStatus> ACTIVE_STATUSES = List.of(
            AdvanceStatus.PENDING,
            AdvanceStatus.DISPATCHER_REVIEW,
            AdvanceStatus.FINANCE_REVIEW,
            AdvanceStatus.APPROVED,
            AdvanceStatus.PAID
    );

    private final AdvanceRepository   advanceRepository;
    private final ReferenceClient     referenceClient;
    private final StringRedisTemplate redisTemplate;

    public AvailableLimitDto checkLimit(UUID driverId, AdvanceType type, BigDecimal requested) {
        String limitKey = LIMIT_KEY_PREFIX + driverId + ":" + type.name();
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();

        BigDecimal monthlyLimit;
        BigDecimal autoApproveThreshold;

        try {
            String monthlyStr = hashOps.get(limitKey, "monthly");
            String thresholdStr = hashOps.get(limitKey, "auto_threshold");

            if (monthlyStr != null && thresholdStr != null) {
                monthlyLimit = new BigDecimal(monthlyStr);
                autoApproveThreshold = new BigDecimal(thresholdStr);
            } else {
                LimitResponseDto limit = loadLimitFromReference(driverId, type);
                monthlyLimit = limit.monthlyLimit();
                autoApproveThreshold = limit.autoApproveThreshold();
                cacheLimitConfig(limitKey, limit, hashOps);
            }
        } catch (LimitExceededException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable for limit lookup, loading from reference: {}", e.getMessage());
            LimitResponseDto limit = loadLimitFromReference(driverId, type);
            monthlyLimit = limit.monthlyLimit();
            autoApproveThreshold = limit.autoApproveThreshold();
        }

        BigDecimal usedAmount = advanceRepository.sumAmountByDriverAndTypeAndStatusIn(
                driverId, type, ACTIVE_STATUSES);
        BigDecimal available = monthlyLimit.subtract(usedAmount);

        if (requested.compareTo(available) > 0) {
            throw new LimitExceededException(driverId.toString(), type, requested, available);
        }

        return new AvailableLimitDto(monthlyLimit, usedAmount, available,
                autoApproveThreshold, requested, true);
    }

    public void applyLimit(UUID driverId, AdvanceType type, BigDecimal amount) {
        String key = LIMIT_USED_KEY_PREFIX + driverId + ":" + type.name();
        long ttlSeconds = secondsUntilEndOfMonth();
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                connection.execute("INCRBYFLOAT", keyBytes,
                        String.valueOf(amount.doubleValue()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                connection.keyCommands().expire(keyBytes, ttlSeconds);
                return null;
            });
        } catch (Exception e) {
            log.warn("Redis unavailable during applyLimit for driver {}: {}", driverId, e.getMessage());
        }
    }

    public void releaseLimit(UUID driverId, AdvanceType type, BigDecimal amount) {
        String key = LIMIT_USED_KEY_PREFIX + driverId + ":" + type.name();
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                connection.execute("INCRBYFLOAT", keyBytes,
                        String.valueOf(-amount.doubleValue()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return null;
            });
        } catch (Exception e) {
            log.warn("Redis unavailable during releaseLimit for driver {}: {}", driverId, e.getMessage());
        }
    }

    @KafkaListener(topics = "#{T(ru.finuniversity.advance.common.util.KafkaTopics).LIMITS_CHANGED}",
                   groupId = "advance-core-limits",
                   containerFactory = "kafkaListenerContainerFactory")
    public void onLimitsChanged(LimitsChangedEvent event) {
        String key = LIMIT_KEY_PREFIX + event.driverId() + ":" + event.advanceType().name();
        try {
            redisTemplate.delete(key);
            log.info("Invalidated limit cache for driver {} type {}", event.driverId(), event.advanceType());
        } catch (Exception e) {
            log.warn("Redis unavailable during cache invalidation for key {}: {}", key, e.getMessage());
        }
    }

    private LimitResponseDto loadLimitFromReference(UUID driverId, AdvanceType type) {
        List<LimitResponseDto> limits = referenceClient.getDriverLimits(driverId);
        return limits.stream()
                .filter(l -> type.name().equals(l.advanceType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No limit configured for driver " + driverId + " type " + type));
    }

    private void cacheLimitConfig(String key, LimitResponseDto limit,
                                   HashOperations<String, String, String> hashOps) {
        try {
            hashOps.put(key, "monthly", limit.monthlyLimit().toPlainString());
            hashOps.put(key, "daily", limit.dailyLimit() != null
                    ? limit.dailyLimit().toPlainString() : "0");
            hashOps.put(key, "auto_threshold", limit.autoApproveThreshold() != null
                    ? limit.autoApproveThreshold().toPlainString() : "0");
            redisTemplate.expire(key, LIMIT_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable during limit cache write: {}", e.getMessage());
        }
    }

    private long secondsUntilEndOfMonth() {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        return java.time.temporal.ChronoUnit.SECONDS.between(
                now.atStartOfDay(ZoneOffset.UTC).toInstant(),
                endOfMonth.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}

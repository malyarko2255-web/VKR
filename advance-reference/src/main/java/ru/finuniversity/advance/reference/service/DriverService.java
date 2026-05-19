package ru.finuniversity.advance.reference.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.common.dto.AdvanceType;
import ru.finuniversity.advance.common.events.LimitsChangedEvent;
import ru.finuniversity.advance.reference.dto.DriverDto;
import ru.finuniversity.advance.reference.dto.LimitUpdateRequest;
import ru.finuniversity.advance.reference.entity.Driver;
import ru.finuniversity.advance.reference.entity.DriverLimit;
import ru.finuniversity.advance.reference.event.ReferenceEventPublisher;
import ru.finuniversity.advance.reference.exception.DriverNotFoundException;
import ru.finuniversity.advance.reference.repository.DriverLimitRepository;
import ru.finuniversity.advance.reference.repository.DriverRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverService {

    private static final String DRIVER_CACHE_PREFIX = "driver:";
    private static final String LIMIT_CACHE_PREFIX  = "limit:";
    private static final Duration DRIVER_TTL = Duration.ofMinutes(10);
    private static final Duration LIMIT_TTL  = Duration.ofMinutes(5);

    private final DriverRepository       driverRepository;
    private final DriverLimitRepository  driverLimitRepository;
    private final StringRedisTemplate    redisTemplate;
    private final ReferenceEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public DriverDto getDriver(UUID driverId) {
        String cacheKey = DRIVER_CACHE_PREFIX + driverId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return parseDriverDtoFromCache(cached, driverId);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for driver lookup {}", driverId);
        }

        Driver driver = driverRepository.findByIdWithLimits(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));

        DriverDto dto = toDto(driver);

        try {
            redisTemplate.opsForValue().set(cacheKey, serializeDriverDto(dto), DRIVER_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable during driver cache write {}", driverId);
        }

        return dto;
    }

    @Transactional
    public void updateLimit(UUID driverId, LimitUpdateRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new DriverNotFoundException(driverId));

        DriverLimit limit = driverLimitRepository
                .findByDriverIdAndAdvanceType(driverId, request.advanceType())
                .orElseGet(() -> DriverLimit.builder().driver(driver).advanceType(request.advanceType()).build());

        limit.setMonthlyLimit(request.monthlyLimit());
        if (request.dailyLimit() != null) {
            limit.setDailyLimit(request.dailyLimit());
        }
        if (request.autoApproveThreshold() != null) {
            limit.setAutoApproveThreshold(request.autoApproveThreshold());
        }
        driverLimitRepository.save(limit);

        evictLimitCache(driverId, request.advanceType());

        LimitsChangedEvent event = new LimitsChangedEvent(
                UUID.randomUUID().toString(),
                null,
                "LIMITS_CHANGED",
                LocalDateTime.now(),
                driverId.toString(),
                AdvanceType.valueOf(request.advanceType()),
                request.monthlyLimit()
        );
        eventPublisher.publishLimitsChanged(event);

        log.info("Updated limit for driver {} type {}: monthly={}", driverId, request.advanceType(), request.monthlyLimit());
    }

    private void evictLimitCache(UUID driverId, String advanceType) {
        String key = LIMIT_CACHE_PREFIX + driverId + ":" + advanceType;
        try {
            redisTemplate.delete(key);
            redisTemplate.delete(DRIVER_CACHE_PREFIX + driverId);
        } catch (Exception e) {
            log.warn("Redis unavailable during cache eviction for driver {}", driverId);
        }
    }

    private DriverDto toDto(Driver driver) {
        String contractorName = driver.getContractor() != null ? driver.getContractor().getName() : null;
        UUID contractorId = driver.getContractor() != null ? driver.getContractor().getId() : null;
        return new DriverDto(
                driver.getId(),
                driver.getUserId(),
                driver.getFullName(),
                driver.getLicenseNo(),
                driver.getPhone(),
                contractorId,
                contractorName,
                Boolean.TRUE.equals(driver.getActive())
        );
    }

    private String serializeDriverDto(DriverDto dto) {
        return dto.id() + "|" + dto.fullName() + "|" + dto.active();
    }

    private DriverDto parseDriverDtoFromCache(String cached, UUID driverId) {
        // Cache is a lightweight hint; on parse failure fall through to DB
        try {
            String[] parts = cached.split("\\|", 3);
            return new DriverDto(
                    UUID.fromString(parts[0]), null, parts[1], null, null, null, null,
                    Boolean.parseBoolean(parts[2])
            );
        } catch (Exception e) {
            log.warn("Failed to parse cached driver {}, fetching from DB", driverId);
            Driver driver = driverRepository.findByIdWithLimits(driverId)
                    .orElseThrow(() -> new DriverNotFoundException(driverId));
            return toDto(driver);
        }
    }
}

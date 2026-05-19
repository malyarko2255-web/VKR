package ru.finuniversity.advance.scoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.common.dto.DriverScoreDto;
import ru.finuniversity.advance.common.events.ScoreUpdatedEvent;
import ru.finuniversity.advance.scoring.client.CoreClient;
import ru.finuniversity.advance.scoring.client.IdentityClient;
import ru.finuniversity.advance.scoring.client.ReferenceClient;
import ru.finuniversity.advance.scoring.dto.IdentityUserDto;
import ru.finuniversity.advance.scoring.dto.TripStatsDto;
import ru.finuniversity.advance.scoring.entity.DriverScore;
import ru.finuniversity.advance.scoring.entity.ScoreHistory;
import ru.finuniversity.advance.scoring.repository.DriverScoreRepository;
import ru.finuniversity.advance.scoring.repository.ScoreHistoryRepository;
import ru.finuniversity.advance.scoring.repository.TelematicsEventRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringCalculatorService {

    private static final String SCORE_KEY_PREFIX = "score:";
    private static final Duration SCORE_TTL = Duration.ofSeconds(1800);

    // Factor weights
    private static final double W_SCHEDULE = 0.30;
    private static final double W_CLOSURE  = 0.25;
    private static final double W_FUEL     = 0.20;
    private static final double W_DEFAULT  = 0.15;
    private static final double W_TENURE   = 0.10;

    // Fuel efficiency baseline (liters / 100 km)
    private static final double FUEL_NORM_L_PER_100KM = 12.0;

    private final ReferenceClient referenceClient;
    private final CoreClient coreClient;
    private final IdentityClient identityClient;
    private final TelematicsEventRepository telematicsEventRepository;
    private final DriverScoreRepository driverScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(cron = "0 */30 * * * *")
    public void recalculateAll() {
        List<UUID> driverIds;
        try {
            driverIds = referenceClient.getAllDriverIds();
        } catch (Exception e) {
            log.error("Failed to fetch driver list from reference service", e);
            return;
        }
        log.info("Starting score recalculation for {} drivers", driverIds.size());
        for (UUID driverId : driverIds) {
            try {
                calculateAndSave(driverId);
            } catch (Exception e) {
                log.error("Failed to calculate score for driver {}", driverId, e);
            }
        }
    }

    @Transactional
    public DriverScoreDto calculateAndSave(UUID driverId) {
        Instant since30d = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant since90d = Instant.now().minus(90, ChronoUnit.DAYS);

        // --- Factor 1: Schedule adherence (30%) ---
        long totalTrips30d = telematicsEventRepository.countSince(driverId, since30d);
        long onScheduleTrips = telematicsEventRepository.countOnSchedule(driverId, since30d);
        int scheduleScore = totalTrips30d == 0 ? 50
                : clamp((int) Math.round(100.0 * onScheduleTrips / totalTrips30d));

        // --- Factor 2: Advance closure rate (25%) ---
        double closureRate;
        try {
            closureRate = Optional.ofNullable(coreClient.getAdvanceClosureRate(driverId)).orElse(0.5);
        } catch (Exception e) {
            closureRate = 0.5;
        }
        int closureScore = clamp((int) Math.round(closureRate * 100));

        // --- Factor 3: Fuel efficiency (20%) ---
        Double avgFuel = telematicsEventRepository.avgFuelPer100km(driverId, since30d);
        int fuelScore;
        if (avgFuel == null || avgFuel == 0.0) {
            fuelScore = 50;
        } else {
            // avgFuel is liters/km; convert to L/100km
            double lPer100km = avgFuel * 100;
            double ratio = FUEL_NORM_L_PER_100KM / lPer100km;
            fuelScore = clamp((int) Math.round(ratio * 100));
        }

        // --- Factor 4: Default history (15%) ---
        int defaultCount;
        try {
            defaultCount = Optional.ofNullable(coreClient.getDefaultCount(driverId, 90)).orElse(0);
        } catch (Exception e) {
            defaultCount = 0;
        }
        int defaultScore = clamp(100 - defaultCount * 20);

        // --- Factor 5: Tenure (10%) ---
        int tenureScore = 50;
        try {
            IdentityUserDto user = identityClient.getUserById(driverId);
            if (user.createdAt() != null) {
                long monthsTenure = ChronoUnit.MONTHS.between(
                        user.createdAt(), Instant.now());
                tenureScore = clamp((int) Math.min(100, monthsTenure * 5));
            }
        } catch (Exception e) {
            log.debug("Could not fetch identity for driver {}", driverId);
        }

        // --- Weighted total ---
        int total = clamp((int) Math.round(
                scheduleScore * W_SCHEDULE
                + closureScore * W_CLOSURE
                + fuelScore * W_FUEL
                + defaultScore * W_DEFAULT
                + tenureScore * W_TENURE));

        int trips = (int) telematicsEventRepository.countByDriverId(driverId);

        // Persist
        driverScoreRepository.upsert(driverId, total, scheduleScore, closureScore,
                fuelScore, defaultScore, tenureScore, trips, Instant.now());

        ScoreHistory history = ScoreHistory.builder()
                .driverId(driverId)
                .totalScore(total)
                .scheduleScore(scheduleScore)
                .advanceClosureScore(closureScore)
                .fuelEfficiencyScore(fuelScore)
                .defaultHistoryScore(defaultScore)
                .tenureScore(tenureScore)
                .calculatedAt(Instant.now())
                .build();
        scoreHistoryRepository.save(history);

        // Redis cache
        try {
            redisTemplate.opsForValue()
                    .set(SCORE_KEY_PREFIX + driverId, String.valueOf(total), SCORE_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable — score not cached for {}", driverId);
        }

        // Kafka event
        try {
            DriverScore current = driverScoreRepository.findByDriverId(driverId).orElse(null);
            Integer oldScore = current != null ? current.getTotalScore() : null;
            ScoreUpdatedEvent event = new ScoreUpdatedEvent(
                    UUID.randomUUID().toString(),
                    driverId.toString(),
                    "SCORE_UPDATED",
                    LocalDateTime.now(),
                    driverId.toString(),
                    oldScore,
                    total,
                    LocalDateTime.now()
            );
            kafkaTemplate.send("score-updated", driverId.toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish ScoreUpdatedEvent for {}", driverId);
        }

        String riskLevel = total >= 80 ? "LOW" : total >= 60 ? "MEDIUM" : "HIGH";
        return new DriverScoreDto(driverId.toString(), total, scheduleScore, closureScore,
                fuelScore, defaultScore, tenureScore, LocalDateTime.now(), riskLevel);
    }

    public Optional<Integer> getCachedScore(UUID driverId) {
        try {
            String val = redisTemplate.opsForValue().get(SCORE_KEY_PREFIX + driverId);
            return val != null ? Optional.of(Integer.parseInt(val)) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}

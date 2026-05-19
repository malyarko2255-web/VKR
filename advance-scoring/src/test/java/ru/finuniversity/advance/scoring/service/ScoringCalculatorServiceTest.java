package ru.finuniversity.advance.scoring.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import ru.finuniversity.advance.common.dto.DriverScoreDto;
import ru.finuniversity.advance.scoring.client.CoreClient;
import ru.finuniversity.advance.scoring.client.IdentityClient;
import ru.finuniversity.advance.scoring.client.ReferenceClient;
import ru.finuniversity.advance.scoring.dto.IdentityUserDto;
import ru.finuniversity.advance.scoring.entity.DriverScore;
import ru.finuniversity.advance.scoring.entity.ScoreHistory;
import ru.finuniversity.advance.scoring.repository.DriverScoreRepository;
import ru.finuniversity.advance.scoring.repository.ScoreHistoryRepository;
import ru.finuniversity.advance.scoring.repository.TelematicsEventRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringCalculatorServiceTest {

    @Mock ReferenceClient referenceClient;
    @Mock CoreClient coreClient;
    @Mock IdentityClient identityClient;
    @Mock TelematicsEventRepository telematicsEventRepository;
    @Mock DriverScoreRepository driverScoreRepository;
    @Mock ScoreHistoryRepository scoreHistoryRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks ScoringCalculatorService service;

    private static final UUID DRIVER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().doNothing().when(valueOps).set(anyString(), anyString(), any());
        lenient().when(driverScoreRepository.findByDriverId(any())).thenReturn(Optional.empty());
        lenient().when(scoreHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().doNothing().when(driverScoreRepository).upsert(any(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any());
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);
    }

    @Test
    void newDriver_returnsBaselineScore_around50() {
        // new driver: no trips, 50% closure, no defaults, just started
        when(telematicsEventRepository.countSince(eq(DRIVER_ID), any())).thenReturn(0L);
        when(telematicsEventRepository.countOnSchedule(eq(DRIVER_ID), any())).thenReturn(0L);
        when(telematicsEventRepository.avgFuelPer100km(eq(DRIVER_ID), any())).thenReturn(0.0);
        when(telematicsEventRepository.countByDriverId(DRIVER_ID)).thenReturn(0L);
        when(coreClient.getAdvanceClosureRate(DRIVER_ID)).thenReturn(0.5);
        when(coreClient.getDefaultCount(eq(DRIVER_ID), anyInt())).thenReturn(0);
        when(identityClient.getUserById(DRIVER_ID)).thenReturn(
                new IdentityUserDto(DRIVER_ID, null, "driver", "New Driver", null, "DRIVER", true,
                        Instant.now().minus(1, ChronoUnit.DAYS)));

        DriverScoreDto result = service.calculateAndSave(DRIVER_ID);

        assertThat(result.totalScore()).isBetween(40, 60);
    }

    @Test
    void perfectDriver_returnsHighScore_above90() {
        // 100% on schedule, 100% closure, very fuel-efficient, no defaults, 3 years tenure
        when(telematicsEventRepository.countSince(eq(DRIVER_ID), any())).thenReturn(100L);
        when(telematicsEventRepository.countOnSchedule(eq(DRIVER_ID), any())).thenReturn(100L);
        // avg fuel: 0.10 L/km → 10 L/100km (better than 12 L/100km norm → score > 100 → clamped 100)
        when(telematicsEventRepository.avgFuelPer100km(eq(DRIVER_ID), any())).thenReturn(0.10);
        when(telematicsEventRepository.countByDriverId(DRIVER_ID)).thenReturn(100L);
        when(coreClient.getAdvanceClosureRate(DRIVER_ID)).thenReturn(1.0);
        when(coreClient.getDefaultCount(eq(DRIVER_ID), anyInt())).thenReturn(0);
        when(identityClient.getUserById(DRIVER_ID)).thenReturn(
                new IdentityUserDto(DRIVER_ID, null, "driver", "Perfect Driver", null, "DRIVER", true,
                        Instant.now().minus(3 * 365, ChronoUnit.DAYS)));

        DriverScoreDto result = service.calculateAndSave(DRIVER_ID);

        assertThat(result.totalScore()).isGreaterThan(90);
    }

    @Test
    void problematicDriver_returnsLowScore_below40() {
        // 0% on schedule, 0% closure, terrible fuel, 3 defaults, 1 day tenure
        when(telematicsEventRepository.countSince(eq(DRIVER_ID), any())).thenReturn(50L);
        when(telematicsEventRepository.countOnSchedule(eq(DRIVER_ID), any())).thenReturn(0L);
        // avg fuel: 0.25 L/km → 25 L/100km (much worse than norm → ratio 0.48 → score 48)
        when(telematicsEventRepository.avgFuelPer100km(eq(DRIVER_ID), any())).thenReturn(0.25);
        when(telematicsEventRepository.countByDriverId(DRIVER_ID)).thenReturn(50L);
        when(coreClient.getAdvanceClosureRate(DRIVER_ID)).thenReturn(0.0);
        when(coreClient.getDefaultCount(eq(DRIVER_ID), anyInt())).thenReturn(5);
        when(identityClient.getUserById(DRIVER_ID)).thenReturn(
                new IdentityUserDto(DRIVER_ID, null, "driver", "Bad Driver", null, "DRIVER", true,
                        Instant.now().minus(1, ChronoUnit.DAYS)));

        DriverScoreDto result = service.calculateAndSave(DRIVER_ID);

        assertThat(result.totalScore()).isLessThan(40);
    }

    @Test
    void winterFuelEfficiency_whenFuelBetterThanNorm_score3is100() {
        // Fuel score when avg consumption is less than norm → score clamped to 100
        when(telematicsEventRepository.countSince(eq(DRIVER_ID), any())).thenReturn(10L);
        when(telematicsEventRepository.countOnSchedule(eq(DRIVER_ID), any())).thenReturn(5L);
        // 0.08 L/km = 8 L/100km — better than 12 L/100km norm → ratio = 12/8 = 1.5 → 150 → clamped 100
        when(telematicsEventRepository.avgFuelPer100km(eq(DRIVER_ID), any())).thenReturn(0.08);
        when(telematicsEventRepository.countByDriverId(DRIVER_ID)).thenReturn(10L);
        when(coreClient.getAdvanceClosureRate(DRIVER_ID)).thenReturn(0.7);
        when(coreClient.getDefaultCount(eq(DRIVER_ID), anyInt())).thenReturn(0);
        when(identityClient.getUserById(DRIVER_ID)).thenThrow(new RuntimeException("unavailable"));

        DriverScoreDto result = service.calculateAndSave(DRIVER_ID);

        assertThat(result.fuelEfficiencyScore()).isEqualTo(100);
    }
}

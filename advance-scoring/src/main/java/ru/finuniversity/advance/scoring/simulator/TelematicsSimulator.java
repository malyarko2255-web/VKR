package ru.finuniversity.advance.scoring.simulator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.dto.TelematicsEventDto;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class TelematicsSimulator {

    private static final Random RANDOM = new Random();

    private static final List<UUID> TEST_DRIVERS = List.of(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            UUID.fromString("33333333-3333-3333-3333-333333333333")
    );

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedRate = 600_000)
    public void simulate() {
        List<TelematicsEventDto> events = TEST_DRIVERS.stream()
                .map(this::generateEvent)
                .toList();
        events.forEach(e -> kafkaTemplate.send("telematics-events", e.driverId().toString(), e));
        log.info("Simulated {} telematics events for dev drivers", events.size());
    }

    private TelematicsEventDto generateEvent(UUID driverId) {
        double distanceKm = 50 + RANDOM.nextDouble() * 150;
        double fuelNorm = 0.10 + RANDOM.nextDouble() * 0.06; // 10-16 L/100km → as L/km
        return new TelematicsEventDto(
                UUID.randomUUID(),
                driverId,
                UUID.randomUUID(),
                "ROUTE-" + (RANDOM.nextInt(10) + 1),
                Instant.now(),
                distanceKm,
                distanceKm * fuelNorm,
                RANDOM.nextInt(5),
                RANDOM.nextInt(3),
                RANDOM.nextDouble() > 0.1,
                "COMPLETED"
        );
    }
}

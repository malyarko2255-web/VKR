package ru.finuniversity.advance.reference.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.dto.TelematicsEventDto;
import ru.finuniversity.advance.reference.client.ScoringClient;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class TelematicsForwardService {

    private static final Random RANDOM = new Random();

    private static final List<UUID> TEST_DRIVERS = List.of(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            UUID.fromString("33333333-3333-3333-3333-333333333333")
    );

    private final ScoringClient scoringClient;

    @Scheduled(fixedRate = 600_000)
    public void forwardSimulatedEvents() {
        List<TelematicsEventDto> events = TEST_DRIVERS.stream()
                .map(this::buildEvent)
                .toList();
        scoringClient.sendTelematicsEvents(events);
        log.info("Forwarded {} simulated telematics events to scoring service", events.size());
    }

    private TelematicsEventDto buildEvent(UUID driverId) {
        double distKm = 40 + RANDOM.nextDouble() * 160;
        double fuelNorm = 0.10 + RANDOM.nextDouble() * 0.05;
        return new TelematicsEventDto(
                UUID.randomUUID(),
                driverId,
                UUID.randomUUID(),
                "ROUTE-" + (RANDOM.nextInt(10) + 1),
                Instant.now(),
                distKm,
                distKm * fuelNorm,
                RANDOM.nextInt(5),
                RANDOM.nextInt(3),
                RANDOM.nextDouble() > 0.1,
                "COMPLETED"
        );
    }
}

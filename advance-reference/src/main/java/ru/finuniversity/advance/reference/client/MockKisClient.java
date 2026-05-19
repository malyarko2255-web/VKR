package ru.finuniversity.advance.reference.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.reference.dto.TripDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class MockKisClient implements KisClient {

    private static final List<TripDto> MOCK_TRIPS = List.of(
        new TripDto("KIS-001", UUID.fromString("d1000000-0000-0000-0000-000000000001"),
                "Москва — Санкт-Петербург", "IN_TRANSIT",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
        new TripDto("KIS-002", UUID.fromString("d1000000-0000-0000-0000-000000000002"),
                "Москва — Екатеринбург", "LOADING",
                LocalDate.now(), LocalDate.now().plusDays(3)),
        new TripDto("KIS-003", UUID.fromString("d1000000-0000-0000-0000-000000000003"),
                "Москва — Казань", "IN_TRANSIT",
                LocalDate.now().minusDays(2), LocalDate.now()),
        new TripDto("KIS-004", UUID.fromString("d1000000-0000-0000-0000-000000000001"),
                "Санкт-Петербург — Казань", "UNLOADING",
                LocalDate.now().minusDays(3), LocalDate.now()),
        new TripDto("KIS-005", UUID.fromString("d1000000-0000-0000-0000-000000000002"),
                "Москва — Новосибирск", "LOADING",
                LocalDate.now(), LocalDate.now().plusDays(5)),
        new TripDto("KIS-006", UUID.fromString("d1000000-0000-0000-0000-000000000003"),
                "Москва — Санкт-Петербург", "RETURN",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
        new TripDto("KIS-007", UUID.fromString("d1000000-0000-0000-0000-000000000001"),
                "Москва — Казань", "IN_TRANSIT",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)),
        new TripDto("KIS-008", UUID.fromString("d1000000-0000-0000-0000-000000000002"),
                "Москва — Екатеринбург", "COMPLETED",
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(2)),
        new TripDto("KIS-009", UUID.fromString("d1000000-0000-0000-0000-000000000003"),
                "Санкт-Петербург — Казань", "COMPLETED",
                LocalDate.now().minusDays(7), LocalDate.now().minusDays(4)),
        new TripDto("KIS-010", UUID.fromString("d1000000-0000-0000-0000-000000000001"),
                "Москва — Новосибирск", "COMPLETED",
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(6))
    );

    @Override
    public List<TripDto> getActiveTrips(UUID driverId) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("KIS mock: fetching trips for driver {}", driverId);
        return MOCK_TRIPS.stream()
                .filter(t -> t.driverId().equals(driverId))
                .toList();
    }
}

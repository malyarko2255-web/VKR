package ru.finuniversity.advance.reference.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.finuniversity.advance.reference.client.KisClient;
import ru.finuniversity.advance.reference.dto.TripDto;
import ru.finuniversity.advance.reference.repository.DriverRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisSyncService {

    private final DriverRepository driverRepository;
    private final KisClient        kisClient;

    @Scheduled(fixedRate = 900_000)
    public void syncFromKis() {
        log.info("Starting KIS sync");
        List<UUID> activeDriverIds = driverRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .map(d -> d.getId())
                .toList();

        int totalTrips = 0;
        for (UUID driverId : activeDriverIds) {
            try {
                List<TripDto> trips = kisClient.getActiveTrips(driverId);
                totalTrips += trips.size();
                log.debug("KIS sync: driver {} has {} active trips", driverId, trips.size());
            } catch (Exception e) {
                log.error("KIS sync failed for driver {}: {}", driverId, e.getMessage());
            }
        }
        log.info("KIS sync complete: {} drivers, {} trips", activeDriverIds.size(), totalTrips);
    }
}

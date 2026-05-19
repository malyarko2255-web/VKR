package ru.finuniversity.advance.core.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.exception.TripCompletedException;
import ru.finuniversity.advance.core.client.ReferenceClient;
import ru.finuniversity.advance.core.dto.TripResponseDto;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripStageValidator {

    private final ReferenceClient referenceClient;

    public void validate(UUID driverId, String tripId, String requestedStage) {
        if (driverId == null) {
            return;
        }
        try {
            List<TripResponseDto> trips = referenceClient.getDriverTrips(driverId);
            if (trips == null || trips.isEmpty()) {
                return;
            }
            TripResponseDto trip = trips.stream()
                    .filter(t -> tripId != null && tripId.equals(t.tripId()))
                    .findFirst()
                    .orElse(null);

            if (trip == null) {
                return;
            }
            if ("COMPLETED".equals(trip.status())) {
                throw new TripCompletedException(tripId);
            }
            if ("RETURN".equals(requestedStage) && "COMPLETED".equals(trip.status())) {
                throw new TripCompletedException(tripId);
            }
        } catch (TripCompletedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("TripStageValidator: could not validate trip {} for driver {}: {}", tripId, driverId, e.getMessage());
        }
    }
}

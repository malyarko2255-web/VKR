package ru.finuniversity.advance.common.dto;

import java.time.Instant;
import java.util.UUID;

public record TelematicsEventDto(
        UUID eventId,
        UUID driverId,
        UUID vehicleId,
        String routeId,
        Instant occurredAt,
        Double distanceKm,
        Double fuelConsumedLiters,
        Integer speedingMinutes,
        Integer harshBrakingCount,
        Boolean onSchedule,
        String tripStage
) {}

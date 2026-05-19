package ru.finuniversity.advance.reference.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TripDto(
        String tripId,
        UUID driverId,
        String routeName,
        String status,
        LocalDate startDate,
        LocalDate endDate
) {}

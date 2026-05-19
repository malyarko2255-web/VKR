package ru.finuniversity.advance.core.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TripResponseDto(
        String tripId,
        UUID driverId,
        String routeName,
        String status,
        LocalDate startDate,
        LocalDate endDate
) {}

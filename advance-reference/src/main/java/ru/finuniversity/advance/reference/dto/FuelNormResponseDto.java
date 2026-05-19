package ru.finuniversity.advance.reference.dto;

import java.math.BigDecimal;

public record FuelNormResponseDto(
        String vehicleCategory,
        BigDecimal baseNorm,
        BigDecimal distanceKm,
        String season,
        BigDecimal adjustedLiters,
        BigDecimal estimatedAmount
) {}

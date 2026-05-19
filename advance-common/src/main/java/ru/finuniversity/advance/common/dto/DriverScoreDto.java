package ru.finuniversity.advance.common.dto;

import java.time.LocalDateTime;

public record DriverScoreDto(
        String driverId,
        Integer totalScore,
        Integer scheduleScore,
        Integer advanceClosureScore,
        Integer fuelEfficiencyScore,
        Integer defaultHistoryScore,
        Integer tenureScore,
        LocalDateTime calculatedAt,
        String riskLevel
) {}

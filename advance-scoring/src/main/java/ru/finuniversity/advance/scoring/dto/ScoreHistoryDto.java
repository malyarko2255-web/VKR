package ru.finuniversity.advance.scoring.dto;

import java.time.Instant;
import java.util.UUID;

public record ScoreHistoryDto(
        Long id,
        UUID driverId,
        Integer totalScore,
        Integer scheduleScore,
        Integer advanceClosureScore,
        Integer fuelEfficiencyScore,
        Integer defaultHistoryScore,
        Integer tenureScore,
        Instant calculatedAt
) {}

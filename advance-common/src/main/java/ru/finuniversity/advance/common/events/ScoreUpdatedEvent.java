package ru.finuniversity.advance.common.events;

import java.time.LocalDateTime;

public record ScoreUpdatedEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        String driverId,
        Integer oldScore,
        Integer newScore,
        LocalDateTime calculatedAt
) implements AdvanceEvent {}

package ru.finuniversity.advance.common.events;

import java.time.LocalDateTime;

public record AdvanceRejectedEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        String rejectedBy,
        String reason
) implements AdvanceEvent {}

package ru.finuniversity.advance.common.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdvanceReportedEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        LocalDateTime reportedAt,
        Integer receiptsCount,
        BigDecimal totalSpent
) implements AdvanceEvent {}

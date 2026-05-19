package ru.finuniversity.advance.common.events;

import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.dto.AdvanceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdvanceCreatedEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        String driverId,
        String routeId,
        AdvanceType advanceType,
        BigDecimal amount,
        AdvanceStatus status
) implements AdvanceEvent {}

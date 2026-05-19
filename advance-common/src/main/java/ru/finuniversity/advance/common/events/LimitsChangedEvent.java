package ru.finuniversity.advance.common.events;

import ru.finuniversity.advance.common.dto.AdvanceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LimitsChangedEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        String driverId,
        AdvanceType advanceType,
        BigDecimal newLimit
) implements AdvanceEvent {}

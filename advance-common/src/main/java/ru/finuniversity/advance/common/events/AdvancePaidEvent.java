package ru.finuniversity.advance.common.events;

import java.time.LocalDateTime;

public record AdvancePaidEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        String paymentId,
        LocalDateTime paidAt,
        String sbpTransactionId
) implements AdvanceEvent {}

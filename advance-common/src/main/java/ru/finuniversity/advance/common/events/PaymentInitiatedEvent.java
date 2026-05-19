package ru.finuniversity.advance.common.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentInitiatedEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        BigDecimal amount,
        String recipientId,
        String recipientPhone
) implements AdvanceEvent {}

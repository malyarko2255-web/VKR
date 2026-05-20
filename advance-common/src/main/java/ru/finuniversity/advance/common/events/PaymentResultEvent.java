package ru.finuniversity.advance.common.events;

import java.time.LocalDateTime;

public record PaymentResultEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        String driverId,
        String paymentId,
        Boolean success,
        String errorMessage,
        String sbpCode
) implements AdvanceEvent {}

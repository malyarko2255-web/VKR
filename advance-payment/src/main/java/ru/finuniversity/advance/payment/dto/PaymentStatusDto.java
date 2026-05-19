package ru.finuniversity.advance.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentStatusDto(
        UUID paymentId,
        UUID advanceId,
        String status,
        BigDecimal amount,
        String sbpTransactionId,
        Instant initiatedAt,
        Instant completedAt,
        String errorMessage
) {}

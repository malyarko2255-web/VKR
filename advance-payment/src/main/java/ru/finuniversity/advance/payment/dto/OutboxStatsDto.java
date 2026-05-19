package ru.finuniversity.advance.payment.dto;

import java.time.Instant;

public record OutboxStatsDto(
        long pendingCount,
        long sentCount,
        long failedCount,
        long deadLetterCount,
        Instant oldestPending
) {}

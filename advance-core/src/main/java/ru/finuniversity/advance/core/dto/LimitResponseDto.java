package ru.finuniversity.advance.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LimitResponseDto(
        UUID id,
        String advanceType,
        BigDecimal monthlyLimit,
        BigDecimal dailyLimit,
        BigDecimal autoApproveThreshold
) {}

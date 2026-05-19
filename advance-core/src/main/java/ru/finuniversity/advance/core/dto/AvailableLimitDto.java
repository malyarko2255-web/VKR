package ru.finuniversity.advance.core.dto;

import java.math.BigDecimal;

public record AvailableLimitDto(
        BigDecimal monthlyLimit,
        BigDecimal usedAmount,
        BigDecimal available,
        BigDecimal autoApproveThreshold,
        BigDecimal requested,
        boolean approved
) {}

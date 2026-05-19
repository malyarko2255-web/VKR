package ru.finuniversity.advance.common.dto;

import java.math.BigDecimal;

public record DriverLimitDto(
        String driverId,
        AdvanceType advanceType,
        BigDecimal monthlyLimit,
        BigDecimal dailyLimit,
        BigDecimal usedThisMonth,
        BigDecimal available,
        BigDecimal autoApproveThreshold
) {}

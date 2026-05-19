package ru.finuniversity.advance.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LimitUpdateRequest(
        @NotBlank String advanceType,
        @NotNull @Positive BigDecimal monthlyLimit,
        @Positive BigDecimal dailyLimit,
        @Positive BigDecimal autoApproveThreshold
) {}

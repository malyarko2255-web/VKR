package ru.finuniversity.advance.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdvanceRequestDto(
        @NotBlank String driverId,
        String routeId,
        String tripId,
        @NotBlank String tripStage,
        @NotNull AdvanceType advanceType,
        @NotNull @Positive BigDecimal amount,
        @Size(max = 500) String notes
) {}

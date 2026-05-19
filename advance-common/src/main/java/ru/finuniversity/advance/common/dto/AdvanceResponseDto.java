package ru.finuniversity.advance.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdvanceResponseDto(
        String id,
        String requestNo,
        String driverId,
        String driverName,
        String routeId,
        String routeName,
        String tripStage,
        AdvanceType advanceType,
        BigDecimal amount,
        BigDecimal limitAvailable,
        AdvanceStatus status,
        Boolean autoApproved,
        Integer scoreAtCreation,
        String dispatcherComment,
        String financeComment,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

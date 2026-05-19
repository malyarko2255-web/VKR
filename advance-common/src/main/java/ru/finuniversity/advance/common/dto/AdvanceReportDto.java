package ru.finuniversity.advance.common.dto;

import java.util.List;

public record AdvanceReportDto(
        String advanceId,
        List<ReceiptDto> receipts,
        String notes
) {}

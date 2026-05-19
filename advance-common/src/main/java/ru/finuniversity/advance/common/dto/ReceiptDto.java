package ru.finuniversity.advance.common.dto;

import java.math.BigDecimal;

public record ReceiptDto(
        String type,
        BigDecimal amount,
        String description
) {}

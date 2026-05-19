package ru.finuniversity.advance.payment.dto;

public record SbpResponse(
        String transactionId,
        boolean success,
        String responseCode
) {}

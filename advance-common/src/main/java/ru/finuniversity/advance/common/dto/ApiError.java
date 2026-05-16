package ru.finuniversity.advance.common.dto;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String message,
        String path,
        String requestId
) {}

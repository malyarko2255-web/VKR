package ru.finuniversity.advance.identity.dto;

import java.util.UUID;

public record AuditLogEntry(
        UUID requestId,
        UUID userId,
        String username,
        String role,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        String ipAddress,
        String userAgent,
        Long durationMs
) {}

package ru.finuniversity.advance.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String channel,
        String templateCode,
        String title,
        String body,
        UUID advanceId,
        String status,
        Boolean isRead,
        Instant createdAt,
        Instant sentAt
) {}

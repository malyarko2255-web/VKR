package ru.finuniversity.advance.common.events;

import ru.finuniversity.advance.common.dto.AdvanceStatus;
import java.time.LocalDateTime;

public record AdvanceApprovedEvent(
        String eventId,
        String advanceId,
        String eventType,
        LocalDateTime occurredAt,
        String approvedBy,
        String comment,
        AdvanceStatus newStatus
) implements AdvanceEvent {}

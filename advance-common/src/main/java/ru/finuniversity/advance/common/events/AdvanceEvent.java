package ru.finuniversity.advance.common.events;

import java.time.LocalDateTime;

public interface AdvanceEvent {
    String eventId();
    String advanceId();
    String eventType();
    LocalDateTime occurredAt();
}

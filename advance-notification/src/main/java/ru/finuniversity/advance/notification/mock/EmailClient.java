package ru.finuniversity.advance.notification.mock;

import java.util.UUID;

public interface EmailClient {
    void send(UUID recipientId, String subject, String body);
}

package ru.finuniversity.advance.notification.mock;

import java.util.UUID;

public interface SmsClient {
    void send(UUID recipientId, String message);
}

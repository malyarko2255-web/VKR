package ru.finuniversity.advance.notification.mock;

import java.util.UUID;

public interface PushClient {
    void send(UUID recipientId, String title, String body);
}

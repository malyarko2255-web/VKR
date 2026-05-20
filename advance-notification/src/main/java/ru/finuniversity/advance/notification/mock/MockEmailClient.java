package ru.finuniversity.advance.notification.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class MockEmailClient implements EmailClient {

    private static final Random RANDOM = new Random();

    @Override
    public void send(UUID recipientId, String subject, String body) {
        simulateLatency();
        if (RANDOM.nextInt(100) < 2) {
            throw new RuntimeException("MockEmailClient: simulated 2% failure for recipient " + recipientId);
        }
        log.info("[EMAIL] → recipientId={} subject='{}' body='{}'", recipientId, subject, body);
    }

    private void simulateLatency() {
        try {
            Thread.sleep(50 + RANDOM.nextInt(151));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

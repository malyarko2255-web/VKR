package ru.finuniversity.advance.notification.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class MockSmsClient implements SmsClient {

    private static final Random RANDOM = new Random();

    @Override
    public void send(UUID recipientId, String message) {
        simulateLatency();
        if (RANDOM.nextInt(100) < 2) {
            throw new RuntimeException("MockSmsClient: simulated 2% failure for recipient " + recipientId);
        }
        log.info("[SMS] → recipientId={} message='{}'", recipientId, message);
    }

    private void simulateLatency() {
        try {
            Thread.sleep(50 + RANDOM.nextInt(151));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

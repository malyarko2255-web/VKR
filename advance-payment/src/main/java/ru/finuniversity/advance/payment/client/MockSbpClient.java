package ru.finuniversity.advance.payment.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.payment.dto.SbpResponse;
import ru.finuniversity.advance.payment.exception.SbpTimeoutException;

import java.math.BigDecimal;
import java.util.Random;

@Slf4j
@Component
public class MockSbpClient {

    private static final Random RANDOM = new Random();
    private static final double SUCCESS_RATE = 0.95;

    public SbpResponse transfer(String phone, BigDecimal amount, String requestId) {
        log.info("SBP transfer: {} → {} RUB, requestId={}", phone, amount, requestId);

        int delayMs = 300 + RANDOM.nextInt(501);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (RANDOM.nextDouble() >= SUCCESS_RATE) {
            throw new SbpTimeoutException("СБП timeout");
        }

        return new SbpResponse(requestId, true, "0");
    }
}

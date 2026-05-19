package ru.finuniversity.advance.reference.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class AdvanceCoreClientFallback implements AdvanceCoreClient {

    @Override
    public BigDecimal getUsedThisMonth(UUID driverId) {
        log.warn("advance-core circuit breaker open, returning 0 for driver {}", driverId);
        return BigDecimal.ZERO;
    }
}

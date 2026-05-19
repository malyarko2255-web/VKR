package ru.finuniversity.advance.scoring.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.scoring.dto.TripStatsDto;

import java.util.UUID;

@Slf4j
@Component
public class CoreClientFallback implements CoreClient {

    @Override
    public TripStatsDto getTripStats(UUID driverId) {
        log.warn("CoreClient fallback getTripStats for {}", driverId);
        return new TripStatsDto(0, 0, 0);
    }

    @Override
    public Double getAdvanceClosureRate(UUID driverId) {
        log.warn("CoreClient fallback getAdvanceClosureRate for {}", driverId);
        return 0.5;
    }

    @Override
    public Integer getDefaultCount(UUID driverId, int days) {
        log.warn("CoreClient fallback getDefaultCount for {}", driverId);
        return 0;
    }
}

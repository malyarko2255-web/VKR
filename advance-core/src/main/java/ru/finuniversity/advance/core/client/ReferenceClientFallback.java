package ru.finuniversity.advance.core.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.core.dto.DriverResponseDto;
import ru.finuniversity.advance.core.dto.LimitResponseDto;
import ru.finuniversity.advance.core.dto.TripResponseDto;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ReferenceClientFallback implements ReferenceClient {

    @Override
    public DriverResponseDto getDriver(UUID id) {
        log.warn("advance-reference circuit open, cannot fetch driver {}", id);
        return null;
    }

    @Override
    public List<LimitResponseDto> getDriverLimits(UUID id) {
        log.warn("advance-reference circuit open, cannot fetch limits for driver {}", id);
        return Collections.emptyList();
    }

    @Override
    public List<TripResponseDto> getDriverTrips(UUID id) {
        log.warn("advance-reference circuit open, cannot fetch trips for driver {}", id);
        return Collections.emptyList();
    }
}

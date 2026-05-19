package ru.finuniversity.advance.reference.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.dto.TelematicsEventDto;

import java.util.List;

@Slf4j
@Component
public class ScoringClientFallback implements ScoringClient {

    @Override
    public void sendTelematicsEvents(List<TelematicsEventDto> events) {
        log.warn("ScoringClient fallback: {} telematics events dropped", events.size());
    }
}

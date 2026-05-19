package ru.finuniversity.advance.reference.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.finuniversity.advance.common.dto.TelematicsEventDto;

import java.util.List;

@FeignClient(name = "advance-scoring", url = "${feign.advance-scoring.url:http://localhost:8084}",
        fallback = ScoringClientFallback.class)
public interface ScoringClient {

    @PostMapping("/api/v1/telematics/events")
    void sendTelematicsEvents(@RequestBody List<TelematicsEventDto> events);
}

package ru.finuniversity.advance.scoring.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.finuniversity.advance.scoring.dto.TripStatsDto;

import java.util.UUID;

@FeignClient(name = "advance-core", url = "${feign.advance-core.url:http://localhost:8082}",
        fallback = CoreClientFallback.class)
public interface CoreClient {

    @GetMapping("/api/v1/advances/drivers/{driverId}/trip-stats")
    TripStatsDto getTripStats(@PathVariable UUID driverId);

    @GetMapping("/api/v1/advances/drivers/{driverId}/closure-rate")
    Double getAdvanceClosureRate(@PathVariable UUID driverId);

    @GetMapping("/api/v1/advances/drivers/{driverId}/default-count")
    Integer getDefaultCount(@PathVariable UUID driverId, @RequestParam(defaultValue = "90") int days);
}

package ru.finuniversity.advance.core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.finuniversity.advance.core.dto.DriverResponseDto;
import ru.finuniversity.advance.core.dto.LimitResponseDto;
import ru.finuniversity.advance.core.dto.TripResponseDto;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "advance-reference",
        url = "${advance.reference.url:http://localhost:8085}",
        fallback = ReferenceClientFallback.class
)
public interface ReferenceClient {

    @GetMapping("/api/v1/drivers/{id}")
    DriverResponseDto getDriver(@PathVariable UUID id);

    @GetMapping("/api/v1/drivers/{id}/limits")
    List<LimitResponseDto> getDriverLimits(@PathVariable UUID id);

    @GetMapping("/api/v1/drivers/{id}/trips")
    List<TripResponseDto> getDriverTrips(@PathVariable UUID id);
}

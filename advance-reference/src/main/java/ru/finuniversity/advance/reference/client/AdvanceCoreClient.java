package ru.finuniversity.advance.reference.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(
        name = "advance-core",
        url = "${advance.core.url:http://localhost:8082}",
        fallback = AdvanceCoreClientFallback.class
)
public interface AdvanceCoreClient {

    @GetMapping("/api/v1/advances/driver/{driverId}/used-this-month")
    BigDecimal getUsedThisMonth(@PathVariable UUID driverId);
}

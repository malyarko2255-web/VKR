package ru.finuniversity.advance.scoring.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import ru.finuniversity.advance.common.dto.DriverScoreDto;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "advance-reference", url = "${feign.advance-reference.url:http://localhost:8085}",
        fallback = ReferenceClientFallback.class)
public interface ReferenceClient {

    @GetMapping("/api/v1/drivers")
    List<UUID> getAllDriverIds();
}

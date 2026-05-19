package ru.finuniversity.advance.scoring.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.finuniversity.advance.scoring.dto.IdentityUserDto;

import java.util.UUID;

@FeignClient(name = "advance-identity", url = "${feign.advance-identity.url:http://localhost:8081}",
        fallback = IdentityClientFallback.class)
public interface IdentityClient {

    @GetMapping("/api/v1/users/{id}")
    IdentityUserDto getUserById(@PathVariable UUID id);
}

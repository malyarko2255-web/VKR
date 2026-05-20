package ru.finuniversity.advance.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "advance-identity",
        url = "${feign.advance-identity.url:http://localhost:8081}",
        fallback = IdentityClientFallback.class)
public interface IdentityClient {

    @GetMapping("/api/v1/users/by-role/{role}")
    List<UUID> getIdsByRole(@PathVariable String role);
}

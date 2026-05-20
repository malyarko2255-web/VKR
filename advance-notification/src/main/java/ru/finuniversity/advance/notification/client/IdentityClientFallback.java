package ru.finuniversity.advance.notification.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class IdentityClientFallback implements IdentityClient {

    @Override
    public List<UUID> getIdsByRole(String role) {
        log.warn("IdentityClient fallback: returning empty list for role={}", role);
        return List.of();
    }
}

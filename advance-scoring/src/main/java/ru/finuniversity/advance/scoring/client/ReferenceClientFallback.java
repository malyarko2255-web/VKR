package ru.finuniversity.advance.scoring.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ReferenceClientFallback implements ReferenceClient {

    @Override
    public List<UUID> getAllDriverIds() {
        log.warn("ReferenceClient fallback: returning empty driver list");
        return List.of();
    }
}

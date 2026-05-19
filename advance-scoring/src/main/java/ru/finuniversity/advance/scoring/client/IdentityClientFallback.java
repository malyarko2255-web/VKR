package ru.finuniversity.advance.scoring.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.scoring.dto.IdentityUserDto;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class IdentityClientFallback implements IdentityClient {

    @Override
    public IdentityUserDto getUserById(UUID id) {
        log.warn("IdentityClient fallback getUserById for {}", id);
        return new IdentityUserDto(id, null, "unknown", "unknown", null, "DRIVER", true, Instant.now());
    }
}

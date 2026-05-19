package ru.finuniversity.advance.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.identity.entity.User;
import ru.finuniversity.advance.identity.entity.UserRole;
import ru.finuniversity.advance.identity.repository.UserRepository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

    private static final String CACHE_PREFIX = "user:sync:";
    private static final Duration CACHE_TTL   = Duration.ofMinutes(5);

    private final UserRepository    userRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public UUID syncUser(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        String cacheKey   = CACHE_PREFIX + keycloakId;

        // Попытка получить из кэша
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return UUID.fromString(cached);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable during cache lookup for {}", keycloakId);
        }

        String   username = jwt.getClaimAsString("preferred_username");
        String   fullName = jwt.getClaimAsString("name");
        String   email    = jwt.getClaimAsString("email");
        UserRole role     = extractRole(jwt);

        Optional<User> existing = userRepository.findByKeycloakId(keycloakId);
        User user;

        if (existing.isEmpty()) {
            user = User.builder()
                    .keycloakId(keycloakId)
                    .username(username)
                    .fullName(fullName)
                    .email(email)
                    .role(role)
                    .active(true)
                    .build();
            user = userRepository.save(user);
            log.info("Created new user: keycloakId={}, username={}", keycloakId, username);
        } else {
            user = existing.get();
            boolean changed = false;

            if (!Objects.equals(user.getFullName(), fullName)) {
                user.setFullName(fullName);
                changed = true;
            }
            if (!Objects.equals(user.getEmail(), email)) {
                user.setEmail(email);
                changed = true;
            }

            if (changed) {
                user = userRepository.save(user);
                log.debug("Updated user: keycloakId={}", keycloakId);
            }
        }

        UUID userId = user.getId();

        // Сохранить в кэш
        try {
            redisTemplate.opsForValue().set(cacheKey, userId.toString(), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable during cache write for {}", keycloakId);
        }

        return userId;
    }

    private UserRole extractRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            for (String r : roles) {
                try {
                    return UserRole.valueOf(r);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return UserRole.DRIVER;
    }
}

package ru.finuniversity.advance.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.finuniversity.advance.identity.entity.User;
import ru.finuniversity.advance.identity.entity.UserRole;
import ru.finuniversity.advance.identity.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {

    @Mock UserRepository         userRepository;
    @Mock StringRedisTemplate    redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks UserSyncService userSyncService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
    }

    @Test
    void syncUser_newUser_createsRecord() {
        Jwt jwt = buildJwt("kc-001", "driver1", "Иван Водитель", "driver1@test.com", "DRIVER");

        when(userRepository.findByKeycloakId("kc-001")).thenReturn(Optional.empty());

        User saved = User.builder().id(UUID.randomUUID()).keycloakId("kc-001")
                .username("driver1").build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UUID result = userSyncService.syncUser(jwt);

        assertThat(result).isEqualTo(saved.getId());
        verify(userRepository).save(argThat(u ->
                "kc-001".equals(u.getKeycloakId()) &&
                "driver1".equals(u.getUsername()) &&
                "Иван Водитель".equals(u.getFullName()) &&
                UserRole.DRIVER == u.getRole()
        ));
    }

    @Test
    void syncUser_existingUser_updatesIfChanged() {
        User existing = User.builder()
                .id(UUID.randomUUID()).keycloakId("kc-002")
                .username("driver2").fullName("Старое Имя")
                .email("d2@test.com").role(UserRole.DRIVER).build();

        Jwt jwt = buildJwt("kc-002", "driver2", "Новое Имя", "d2@test.com", "DRIVER");

        when(userRepository.findByKeycloakId("kc-002")).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenReturn(existing);

        userSyncService.syncUser(jwt);

        verify(userRepository).save(argThat(u -> "Новое Имя".equals(u.getFullName())));
    }

    @Test
    void syncUser_existingUser_noUpdateIfSame() {
        User existing = User.builder()
                .id(UUID.randomUUID()).keycloakId("kc-003")
                .username("driver3").fullName("Пётр Петров")
                .email("d3@test.com").role(UserRole.DRIVER).build();

        Jwt jwt = buildJwt("kc-003", "driver3", "Пётр Петров", "d3@test.com", "DRIVER");

        when(userRepository.findByKeycloakId("kc-003")).thenReturn(Optional.of(existing));

        userSyncService.syncUser(jwt);

        verify(userRepository, never()).save(any());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Jwt buildJwt(String sub, String username, String name,
                          String email, String role) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(sub)
                .claim("preferred_username", username)
                .claim("name", name)
                .claim("email", email)
                .claim("roles", List.of(role))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}

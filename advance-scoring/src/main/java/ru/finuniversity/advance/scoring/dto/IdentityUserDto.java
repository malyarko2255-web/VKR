package ru.finuniversity.advance.scoring.dto;

import java.time.Instant;
import java.util.UUID;

public record IdentityUserDto(
        UUID id,
        String keycloakId,
        String username,
        String fullName,
        String email,
        String role,
        Boolean active,
        Instant createdAt
) {}

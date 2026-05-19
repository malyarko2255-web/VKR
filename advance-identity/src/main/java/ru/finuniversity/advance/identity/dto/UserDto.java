package ru.finuniversity.advance.identity.dto;

import ru.finuniversity.advance.identity.entity.UserRole;

import java.util.UUID;

public record UserDto(
        UUID id,
        String keycloakId,
        String username,
        String fullName,
        String email,
        UserRole role,
        Boolean active
) {}

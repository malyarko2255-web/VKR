package ru.finuniversity.advance.core.dto;

import java.util.UUID;

public record DriverResponseDto(
        UUID id,
        UUID userId,
        String fullName,
        String licenseNo,
        String phone,
        UUID contractorId,
        String contractorName,
        boolean active
) {}

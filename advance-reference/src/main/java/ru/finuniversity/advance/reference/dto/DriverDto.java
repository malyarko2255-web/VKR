package ru.finuniversity.advance.reference.dto;

import java.util.UUID;

public record DriverDto(
        UUID id,
        UUID userId,
        String fullName,
        String licenseNo,
        String phone,
        UUID contractorId,
        String contractorName,
        boolean active
) {}

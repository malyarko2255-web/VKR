package ru.finuniversity.advance.common.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequestDto(@NotBlank String reason) {}

package ru.finuniversity.advance.identity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.finuniversity.advance.common.exception.AdvanceNotFoundException;
import ru.finuniversity.advance.identity.dto.StatusUpdateRequest;
import ru.finuniversity.advance.identity.dto.UserDto;
import ru.finuniversity.advance.identity.entity.User;
import ru.finuniversity.advance.identity.repository.UserRepository;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public UserDto getMe(@AuthenticationPrincipal Jwt jwt) {
        return userRepository.findByKeycloakId(jwt.getSubject())
                .map(this::toDto)
                .orElseThrow(() -> new AdvanceNotFoundException(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_DIRECTOR')")
    public UserDto getById(@PathVariable UUID id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new AdvanceNotFoundException(id.toString()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id,
                                             @RequestBody StatusUpdateRequest body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AdvanceNotFoundException(id.toString()));
        user.setActive(body.active());
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getKeycloakId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getActive()
        );
    }
}

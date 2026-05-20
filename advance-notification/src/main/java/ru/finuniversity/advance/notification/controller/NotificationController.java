package ru.finuniversity.advance.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.finuniversity.advance.notification.dto.NotificationDto;
import ru.finuniversity.advance.notification.entity.Notification;
import ru.finuniversity.advance.notification.repository.NotificationRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/my")
    public Page<NotificationDto> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> results = unreadOnly
                ? notificationRepository.findByRecipientIdAndChannelAndIsReadFalseOrderByCreatedAtDesc(
                        userId, "IN_APP", pageable)
                : notificationRepository.findByRecipientIdAndChannelOrderByCreatedAtDesc(
                        userId, "IN_APP", pageable);
        return results.map(this::toDto);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id,
                                          @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationRepository.findById(id).ifPresent(n -> {
            if (userId.equals(n.getRecipientId()) && Boolean.FALSE.equals(n.getIsRead())) {
                n.setIsRead(true);
                n.setReadAt(Instant.now());
                notificationRepository.save(n);
            }
        });
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        notificationRepository.markAllReadForUser(userId, Instant.now());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        long count = notificationRepository.countByRecipientIdAndChannelAndIsReadFalse(userId, "IN_APP");
        return Map.of("unreadCount", count);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getChannel(),
                n.getTemplateCode(),
                n.getTitle(),
                n.getBody(),
                n.getAdvanceId(),
                n.getStatus(),
                n.getIsRead(),
                n.getCreatedAt(),
                n.getSentAt()
        );
    }
}

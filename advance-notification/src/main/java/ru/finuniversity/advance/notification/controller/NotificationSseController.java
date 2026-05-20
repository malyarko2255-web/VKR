package ru.finuniversity.advance.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.finuniversity.advance.notification.dto.NotificationDto;
import ru.finuniversity.advance.notification.entity.Notification;
import ru.finuniversity.advance.notification.repository.NotificationRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationSseController {

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private final NotificationRepository notificationRepository;

    private final ConcurrentHashMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Instant> lastSentAt = new ConcurrentHashMap<>();

    @GetMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitters.put(userId, emitter);
        lastSentAt.put(userId, Instant.now().minus(1, ChronoUnit.DAYS));

        emitter.onCompletion(() -> cleanup(userId));
        emitter.onTimeout(() -> cleanup(userId));
        emitter.onError(e -> cleanup(userId));

        // Send all pending unread IN_APP notifications on connect
        List<Notification> pending = notificationRepository
                .findByRecipientIdAndChannelAndCreatedAtAfterAndIsReadFalse(
                        userId, "IN_APP", Instant.now().minus(7, ChronoUnit.DAYS));
        for (Notification n : pending) {
            sendToEmitter(userId, emitter, n);
        }
        if (!pending.isEmpty()) {
            lastSentAt.put(userId, Instant.now());
        }

        return emitter;
    }

    @Scheduled(fixedDelay = 5000)
    public void pushNewNotifications() {
        emitters.forEach((userId, emitter) -> {
            Instant since = lastSentAt.getOrDefault(userId, Instant.now().minus(1, ChronoUnit.HOURS));
            List<Notification> newNotifs = notificationRepository
                    .findByRecipientIdAndChannelAndCreatedAtAfterAndIsReadFalse(userId, "IN_APP", since);
            if (!newNotifs.isEmpty()) {
                newNotifs.forEach(n -> sendToEmitter(userId, emitter, n));
                lastSentAt.put(userId, Instant.now());
            }
        });
    }

    private void sendToEmitter(UUID userId, SseEmitter emitter, Notification n) {
        try {
            NotificationDto dto = toDto(n);
            emitter.send(SseEmitter.event()
                    .id(n.getId().toString())
                    .name("notification")
                    .data(dto));
        } catch (IOException e) {
            log.debug("SSE send failed for user={}: {}", userId, e.getMessage());
            cleanup(userId);
        }
    }

    private void cleanup(UUID userId) {
        emitters.remove(userId);
        lastSentAt.remove(userId);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(), n.getChannel(), n.getTemplateCode(),
                n.getTitle(), n.getBody(), n.getAdvanceId(),
                n.getStatus(), n.getIsRead(), n.getCreatedAt(), n.getSentAt()
        );
    }
}

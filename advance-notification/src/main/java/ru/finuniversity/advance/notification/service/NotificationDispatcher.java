package ru.finuniversity.advance.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.finuniversity.advance.notification.entity.Notification;
import ru.finuniversity.advance.notification.mock.EmailClient;
import ru.finuniversity.advance.notification.mock.PushClient;
import ru.finuniversity.advance.notification.mock.SmsClient;
import ru.finuniversity.advance.notification.repository.NotificationRepository;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final PushClient pushClient;
    private final SmsClient smsClient;
    private final EmailClient emailClient;
    private final NotificationRepository notificationRepository;

    public void send(Notification notification) {
        try {
            switch (notification.getChannel()) {
                case "PUSH" -> pushClient.send(
                        notification.getRecipientId(),
                        notification.getTitle(),
                        notification.getBody());
                case "SMS" -> smsClient.send(
                        notification.getRecipientId(),
                        notification.getBody());
                case "EMAIL" -> emailClient.send(
                        notification.getRecipientId(),
                        notification.getTitle(),
                        notification.getBody());
                case "IN_APP" -> {
                    notification.setStatus("DELIVERED");
                    notificationRepository.save(notification);
                    log.debug("[IN_APP] stored for recipientId={}", notification.getRecipientId());
                    return;
                }
                default -> log.warn("Unknown channel: {}", notification.getChannel());
            }
            notification.setStatus("SENT");
            notification.setSentAt(Instant.now());
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            log.error("Failed to send {} notification to {}: {}",
                    notification.getChannel(), notification.getRecipientId(), e.getMessage());
        }
        notificationRepository.save(notification);
    }
}

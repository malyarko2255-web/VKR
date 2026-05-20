package ru.finuniversity.advance.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.common.events.*;
import ru.finuniversity.advance.notification.client.IdentityClient;
import ru.finuniversity.advance.notification.entity.Notification;
import ru.finuniversity.advance.notification.entity.NotificationTemplate;
import ru.finuniversity.advance.notification.repository.NotificationRepository;
import ru.finuniversity.advance.notification.repository.NotificationTemplateRepository;
import ru.finuniversity.advance.notification.util.TemplateEngine;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDispatcher dispatcher;
    private final TemplateEngine templateEngine;
    private final IdentityClient identityClient;

    @Transactional
    public void process(AdvanceEvent event) {
        String templateCode = resolveTemplateCode(event);
        if (templateCode == null) {
            log.debug("No template for event type {}, skipping", event.getClass().getSimpleName());
            return;
        }

        List<UUID> recipientIds = resolveRecipients(event, templateCode);
        if (recipientIds.isEmpty()) {
            log.warn("No recipients for templateCode={} advanceId={}", templateCode, event.advanceId());
            return;
        }

        List<NotificationTemplate> templates = templateRepository.findByCodeAndActiveTrue(templateCode);
        if (templates.isEmpty()) {
            log.warn("No active templates for code={}", templateCode);
            return;
        }

        Map<String, String> vars = buildVars(event);
        UUID advanceId = parseUuid(event.advanceId());

        for (NotificationTemplate tmpl : templates) {
            for (UUID recipientId : recipientIds) {
                String title = templateEngine.render(tmpl.getTitleTemplate(), vars);
                String body  = templateEngine.render(tmpl.getBodyTemplate(), vars);

                Notification notif = Notification.builder()
                        .recipientId(recipientId)
                        .channel(tmpl.getChannel())
                        .templateCode(tmpl.getCode())
                        .title(title)
                        .body(body)
                        .advanceId(advanceId)
                        .build();
                notif = notificationRepository.save(notif);
                dispatcher.send(notif);
            }
        }
    }

    private String resolveTemplateCode(AdvanceEvent event) {
        return switch (event.getClass().getSimpleName()) {
            case "AdvanceCreatedEvent"  -> "ADVANCE_CREATED";
            case "AdvanceApprovedEvent" -> "ADVANCE_APPROVED";
            case "AdvanceRejectedEvent" -> "ADVANCE_REJECTED";
            case "PaymentResultEvent"   -> {
                PaymentResultEvent e = (PaymentResultEvent) event;
                yield Boolean.TRUE.equals(e.success()) ? "ADVANCE_PAID" : null;
            }
            default -> null;
        };
    }

    private List<UUID> resolveRecipients(AdvanceEvent event, String templateCode) {
        return switch (templateCode) {
            case "ADVANCE_CREATED" -> {
                try {
                    yield identityClient.getIdsByRole("DISPATCHER");
                } catch (Exception e) {
                    log.warn("Could not fetch dispatcher IDs: {}", e.getMessage());
                    yield List.of();
                }
            }
            default -> {
                String driverId = extractDriverId(event);
                yield driverId != null ? List.of(UUID.fromString(driverId)) : List.of();
            }
        };
    }

    private String extractDriverId(AdvanceEvent event) {
        if (event instanceof AdvanceCreatedEvent  e) return e.driverId();
        if (event instanceof AdvanceApprovedEvent e) return e.driverId();
        if (event instanceof AdvanceRejectedEvent e) return e.driverId();
        if (event instanceof PaymentResultEvent   e) return e.driverId();
        return null;
    }

    private Map<String, String> buildVars(AdvanceEvent event) {
        Map<String, String> vars = new HashMap<>();
        vars.put("advance_id", event.advanceId());

        if (event instanceof AdvanceCreatedEvent e) {
            vars.put("request_no",    e.advanceId());
            vars.put("advance_type",  e.advanceType() != null ? e.advanceType().name() : "");
            vars.put("amount",        e.amount() != null ? e.amount().toPlainString() : "");
            vars.put("driver_id",     e.driverId());
        } else if (event instanceof AdvanceApprovedEvent e) {
            vars.put("request_no",    e.advanceId());
            vars.put("approved_by",   e.approvedBy());
            vars.put("comment",       e.comment() != null ? e.comment() : "");
            vars.put("amount",        "");
        } else if (event instanceof AdvanceRejectedEvent e) {
            vars.put("request_no",      e.advanceId());
            vars.put("rejection_reason", e.reason() != null ? e.reason() : "");
            vars.put("rejected_by",     e.rejectedBy());
        } else if (event instanceof PaymentResultEvent e) {
            vars.put("request_no",    e.advanceId());
            vars.put("payment_id",    e.paymentId() != null ? e.paymentId() : "");
            vars.put("amount",        "");
            vars.put("bank_name",     "СБП");
        }
        return vars;
    }

    private UUID parseUuid(String s) {
        try {
            return s != null ? UUID.fromString(s) : null;
        } catch (Exception e) {
            return null;
        }
    }
}

package ru.finuniversity.advance.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.events.*;
import ru.finuniversity.advance.common.util.KafkaTopics;
import ru.finuniversity.advance.notification.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {
                KafkaTopics.ADVANCES_CREATED,
                KafkaTopics.ADVANCES_APPROVED,
                KafkaTopics.ADVANCES_REJECTED,
                KafkaTopics.ADVANCES_PAYMENT_RESULTS
            },
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleEvent(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String json  = record.value();
        log.debug("Received event from topic={} key={}", topic, record.key());

        try {
            AdvanceEvent event = deserialize(topic, json);
            if (event != null) {
                notificationService.process(event);
            }
        } catch (Exception e) {
            log.error("Error processing event from topic={}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Notification processing failed", e);
        }
    }

    private AdvanceEvent deserialize(String topic, String json) throws Exception {
        return switch (topic) {
            case KafkaTopics.ADVANCES_CREATED         -> objectMapper.readValue(json, AdvanceCreatedEvent.class);
            case KafkaTopics.ADVANCES_APPROVED        -> objectMapper.readValue(json, AdvanceApprovedEvent.class);
            case KafkaTopics.ADVANCES_REJECTED        -> objectMapper.readValue(json, AdvanceRejectedEvent.class);
            case KafkaTopics.ADVANCES_PAYMENT_RESULTS -> objectMapper.readValue(json, PaymentResultEvent.class);
            default -> {
                log.warn("Unknown topic: {}", topic);
                yield null;
            }
        };
    }
}

package ru.finuniversity.advance.core.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.events.*;
import ru.finuniversity.advance.common.util.KafkaTopics;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to {}: {}", topic, ex.getMessage());
                    } else {
                        log.debug("Published event to topic={} key={}", topic, key);
                    }
                });
    }

    public void publishAdvanceCreated(AdvanceCreatedEvent event) {
        publish(KafkaTopics.ADVANCES_CREATED, event.advanceId(), event);
    }

    public void publishAdvanceApproved(AdvanceApprovedEvent event) {
        publish(KafkaTopics.ADVANCES_APPROVED, event.advanceId(), event);
    }

    public void publishAdvanceRejected(AdvanceRejectedEvent event) {
        publish(KafkaTopics.ADVANCES_REJECTED, event.advanceId(), event);
    }

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        publish(KafkaTopics.PAYMENTS_COMMANDS, event.advanceId(), event);
    }
}

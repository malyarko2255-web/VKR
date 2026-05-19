package ru.finuniversity.advance.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.common.events.PaymentResultEvent;
import ru.finuniversity.advance.common.util.KafkaTopics;
import ru.finuniversity.advance.payment.entity.OutboxEvent;
import ru.finuniversity.advance.payment.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository       outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper                objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPendingForUpdate();
        if (events.isEmpty()) {
            return;
        }
        log.debug("OutboxPublisher: processing {} pending events", events.size());

        for (OutboxEvent event : events) {
            try {
                PaymentResultEvent payload = objectMapper.readValue(
                        event.getPayload(), PaymentResultEvent.class);

                kafkaTemplate.send(KafkaTopics.ADVANCES_PAYMENT_RESULTS,
                        event.getAggregateId().toString(), payload).get();

                event.setStatus("SENT");
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);

                log.debug("OutboxEvent {} sent to Kafka", event.getId());
            } catch (Exception e) {
                int retries = event.getRetryCount() + 1;
                event.setRetryCount(retries);
                event.setLastError(e.getMessage());
                if (retries >= MAX_RETRIES) {
                    event.setStatus("DEAD_LETTER");
                    log.error("OutboxEvent {} moved to DEAD_LETTER after {} retries: {}",
                            event.getId(), retries, e.getMessage());
                } else {
                    log.warn("OutboxEvent {} failed (retry {}/{}): {}",
                            event.getId(), retries, MAX_RETRIES, e.getMessage());
                }
                outboxEventRepository.save(event);
            }
        }
    }
}

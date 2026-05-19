package ru.finuniversity.advance.reference.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.events.LimitsChangedEvent;
import ru.finuniversity.advance.common.util.KafkaTopics;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferenceEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLimitsChanged(LimitsChangedEvent event) {
        kafkaTemplate.send(KafkaTopics.LIMITS_CHANGED, event.driverId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish LimitsChangedEvent for driver {}: {}",
                                event.driverId(), ex.getMessage());
                    } else {
                        log.debug("Published LimitsChangedEvent for driver {}", event.driverId());
                    }
                });
    }
}

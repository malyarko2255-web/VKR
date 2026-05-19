package ru.finuniversity.advance.scoring.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.finuniversity.advance.common.dto.TelematicsEventDto;
import ru.finuniversity.advance.scoring.entity.TelematicsEvent;
import ru.finuniversity.advance.scoring.repository.TelematicsEventRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelematicsEventConsumer {

    private static final int FLUSH_THRESHOLD = 100;

    private final TelematicsEventRepository telematicsEventRepository;
    private final List<TelematicsEventDto> buffer = new ArrayList<>();

    @KafkaListener(topics = "telematics-events", groupId = "scoring-group",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(List<TelematicsEventDto> events) {
        synchronized (buffer) {
            buffer.addAll(events);
            if (buffer.size() >= FLUSH_THRESHOLD) {
                flush();
            }
        }
        log.debug("Buffered {} telematics events, buffer size={}", events.size(), buffer.size());
    }

    @Scheduled(fixedDelay = 2000)
    public void scheduledFlush() {
        synchronized (buffer) {
            if (!buffer.isEmpty()) {
                flush();
            }
        }
    }

    private void flush() {
        List<TelematicsEventDto> toSave = new ArrayList<>(buffer);
        buffer.clear();
        List<TelematicsEvent> entities = toSave.stream()
                .map(dto -> TelematicsEvent.builder()
                        .driverId(dto.driverId())
                        .vehicleId(dto.vehicleId())
                        .routeId(dto.routeId())
                        .occurredAt(dto.occurredAt())
                        .distanceKm(dto.distanceKm())
                        .fuelConsumedLiters(dto.fuelConsumedLiters())
                        .speedingMinutes(dto.speedingMinutes() != null ? dto.speedingMinutes() : 0)
                        .harshBrakingCount(dto.harshBrakingCount() != null ? dto.harshBrakingCount() : 0)
                        .onSchedule(dto.onSchedule() != null ? dto.onSchedule() : true)
                        .tripStage(dto.tripStage())
                        .build())
                .toList();
        telematicsEventRepository.saveAll(entities);
        log.info("Flushed {} telematics events to DB", entities.size());
    }
}

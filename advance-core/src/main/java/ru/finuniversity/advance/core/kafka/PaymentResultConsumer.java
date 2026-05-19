package ru.finuniversity.advance.core.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.events.AdvancePaidEvent;
import ru.finuniversity.advance.common.events.PaymentResultEvent;
import ru.finuniversity.advance.common.util.KafkaTopics;
import ru.finuniversity.advance.core.entity.Advance;
import ru.finuniversity.advance.core.entity.AdvanceHistory;
import ru.finuniversity.advance.core.repository.AdvanceHistoryRepository;
import ru.finuniversity.advance.core.repository.AdvanceRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultConsumer {

    private final AdvanceRepository       advanceRepository;
    private final AdvanceHistoryRepository historyRepository;
    private final CoreEventPublisher      eventPublisher;

    @KafkaListener(topics = KafkaTopics.ADVANCES_PAYMENT_RESULTS,
                   groupId = "advance-core-payments",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onPaymentResult(PaymentResultEvent event) {
        UUID advanceId = UUID.fromString(event.advanceId());

        Advance advance = advanceRepository.findById(advanceId).orElse(null);
        if (advance == null) {
            log.error("PaymentResultConsumer: advance not found: {}", advanceId);
            return;
        }

        AdvanceStatus prev = advance.getStatus();

        if (Boolean.TRUE.equals(event.success())) {
            advance.setStatus(AdvanceStatus.PAID);
            advanceRepository.save(advance);

            historyRepository.save(AdvanceHistory.builder()
                    .advance(advance)
                    .fromStatus(prev.name())
                    .toStatus(AdvanceStatus.PAID.name())
                    .comment("Payment confirmed: " + event.paymentId())
                    .build());

            eventPublisher.publish(KafkaTopics.NOTIFICATIONS_OUTBOX, advanceId.toString(),
                    new AdvancePaidEvent(
                            UUID.randomUUID().toString(),
                            advanceId.toString(),
                            "ADVANCE_PAID",
                            LocalDateTime.now(),
                            event.paymentId(),
                            LocalDateTime.now(),
                            event.sbpCode()
                    ));
            log.info("Advance {} marked as PAID, paymentId={}", advanceId, event.paymentId());
        } else {
            advance.setStatus(AdvanceStatus.PAYMENT_FAILED);
            advanceRepository.save(advance);

            historyRepository.save(AdvanceHistory.builder()
                    .advance(advance)
                    .fromStatus(prev.name())
                    .toStatus(AdvanceStatus.PAYMENT_FAILED.name())
                    .comment("Payment failed: " + event.errorMessage())
                    .build());

            log.warn("Advance {} payment FAILED: {}", advanceId, event.errorMessage());
        }
    }
}

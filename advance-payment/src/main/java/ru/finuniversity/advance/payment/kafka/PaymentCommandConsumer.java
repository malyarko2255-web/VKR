package ru.finuniversity.advance.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.common.events.PaymentInitiatedEvent;
import ru.finuniversity.advance.common.events.PaymentResultEvent;
import ru.finuniversity.advance.common.util.KafkaTopics;
import ru.finuniversity.advance.payment.entity.OutboxEvent;
import ru.finuniversity.advance.payment.entity.Payment;
import ru.finuniversity.advance.payment.repository.OutboxEventRepository;
import ru.finuniversity.advance.payment.repository.PaymentRepository;
import ru.finuniversity.advance.payment.service.SbpPaymentService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCommandConsumer {

    private final PaymentRepository     paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SbpPaymentService     sbpPaymentService;
    private final ObjectMapper          objectMapper;

    @KafkaListener(topics = KafkaTopics.PAYMENTS_COMMANDS,
                   groupId = "advance-payment-group",
                   containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handlePaymentCommand(PaymentInitiatedEvent event) {
        UUID advanceId = UUID.fromString(event.advanceId());
        log.info("Received payment command: advanceId={} amount={}", advanceId, event.amount());

        // Idempotency check
        Optional<Payment> existing = paymentRepository.findByAdvanceId(advanceId);
        if (existing.isPresent() && "COMPLETED".equals(existing.get().getStatus())) {
            log.info("Payment already COMPLETED for advanceId={}, skipping", advanceId);
            return;
        }

        Payment payment;
        if (existing.isPresent()) {
            payment = existing.get();
        } else {
            payment = Payment.builder()
                    .advanceId(advanceId)
                    .amount(event.amount())
                    .recipientId(UUID.fromString(event.recipientId()))
                    .recipientPhone(event.recipientPhone())
                    .status("PENDING")
                    .build();
        }

        sbpPaymentService.initiatePayment(payment);
        paymentRepository.save(payment);

        boolean success = "COMPLETED".equals(payment.getStatus());
        PaymentResultEvent resultEvent = new PaymentResultEvent(
                UUID.randomUUID().toString(),
                event.advanceId(),
                "PAYMENT_RESULT",
                LocalDateTime.now(),
                payment.getRecipientId().toString(),
                payment.getId().toString(),
                success,
                payment.getErrorMessage(),
                payment.getSbpResponseCode()
        );

        String payload;
        try {
            payload = objectMapper.writeValueAsString(resultEvent);
        } catch (Exception e) {
            log.error("Failed to serialize PaymentResultEvent: {}", e.getMessage());
            payload = "{}";
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateId(advanceId)
                .eventType("PAYMENT_RESULT")
                .payload(payload)
                .build();
        outboxEventRepository.save(outboxEvent);

        log.info("Payment processed: advanceId={} status={} outboxId={}",
                advanceId, payment.getStatus(), outboxEvent.getId());
    }
}

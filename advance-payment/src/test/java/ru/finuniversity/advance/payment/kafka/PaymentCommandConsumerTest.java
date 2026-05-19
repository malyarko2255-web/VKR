package ru.finuniversity.advance.payment.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import ru.finuniversity.advance.common.events.PaymentInitiatedEvent;
import ru.finuniversity.advance.payment.entity.Payment;
import ru.finuniversity.advance.payment.repository.OutboxEventRepository;
import ru.finuniversity.advance.payment.repository.PaymentRepository;
import ru.finuniversity.advance.payment.service.SbpPaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(
    properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:9999"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PaymentCommandConsumerTest {

    @Autowired PaymentCommandConsumer  consumer;
    @Autowired PaymentRepository       paymentRepository;
    @Autowired OutboxEventRepository   outboxEventRepository;

    @MockBean SbpPaymentService        sbpPaymentService;
    @MockBean KafkaTemplate<String, Object> kafkaTemplate;
    @MockBean JwtDecoder               jwtDecoder;
    @MockBean OutboxPublisher          outboxPublisher;

    @BeforeEach
    void clean() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();

        // Stub SBP to always succeed
        doAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setStatus("COMPLETED");
            return null;
        }).when(sbpPaymentService).initiatePayment(any(Payment.class));
    }

    @Test
    void handlePayment_success_createsPaymentAndOutboxEvent() {
        UUID advanceId = UUID.randomUUID();
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                UUID.randomUUID().toString(),
                advanceId.toString(),
                "PAYMENT_INITIATED",
                LocalDateTime.now(),
                BigDecimal.valueOf(15000),
                UUID.randomUUID().toString(),
                "+7-916-000-00-01"
        );

        consumer.handlePaymentCommand(event);

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAdvanceId()).isEqualTo(advanceId);
        assertThat(payments.get(0).getStatus()).isEqualTo("COMPLETED");

        assertThat(outboxEventRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll().get(0).getEventType()).isEqualTo("PAYMENT_RESULT");
    }

    @Test
    void handlePayment_duplicate_noSecondPayment() {
        UUID advanceId = UUID.randomUUID();
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
                UUID.randomUUID().toString(),
                advanceId.toString(),
                "PAYMENT_INITIATED",
                LocalDateTime.now(),
                BigDecimal.valueOf(8000),
                UUID.randomUUID().toString(),
                "+7-916-000-00-02"
        );

        consumer.handlePaymentCommand(event);
        consumer.handlePaymentCommand(event);

        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }
}

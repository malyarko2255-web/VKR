package ru.finuniversity.advance.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ru.finuniversity.advance.common.events.PaymentResultEvent;
import ru.finuniversity.advance.payment.entity.OutboxEvent;
import ru.finuniversity.advance.payment.kafka.OutboxPublisher;
import ru.finuniversity.advance.payment.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks OutboxPublisher outboxPublisher;

    private ObjectMapper objectMapper;
    private String samplePayload;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // Inject real ObjectMapper via reflection (InjectMocks can't inject it automatically alongside mocks)
        var field = OutboxPublisher.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(outboxPublisher, objectMapper);

        PaymentResultEvent event = new PaymentResultEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                "PAYMENT_RESULT",
                LocalDateTime.now(),
                UUID.randomUUID().toString(),
                true, null, "0"
        );
        samplePayload = objectMapper.writeValueAsString(event);
    }

    private OutboxEvent buildEvent(int retryCount) {
        OutboxEvent e = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .eventType("PAYMENT_RESULT")
                .payload(samplePayload)
                .status("PENDING")
                .retryCount(retryCount)
                .build();
        return e;
    }

    @Test
    void publish_successfulKafkaSend_marksSent() {
        OutboxEvent event = buildEvent(0);
        when(outboxEventRepository.findPendingForUpdate()).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo("SENT");
        assertThat(event.getProcessedAt()).isNotNull();
    }

    @Test
    void publish_kafkaFails_incrementsRetryCount() {
        OutboxEvent event = buildEvent(0);
        when(outboxEventRepository.findPendingForUpdate()).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getLastError()).isNotBlank();
    }

    @Test
    void publish_threeFailures_movesToDeadLetter() {
        OutboxEvent event = buildEvent(2);
        when(outboxEventRepository.findPendingForUpdate()).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo("DEAD_LETTER");
    }
}

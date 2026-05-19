package ru.finuniversity.advance.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.finuniversity.advance.payment.dto.OutboxStatsDto;
import ru.finuniversity.advance.payment.dto.PaymentStatusDto;
import ru.finuniversity.advance.payment.entity.Payment;
import ru.finuniversity.advance.payment.repository.OutboxEventRepository;
import ru.finuniversity.advance.payment.repository.PaymentRepository;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment status and outbox management")
public class PaymentController {

    private final PaymentRepository     paymentRepository;
    private final OutboxEventRepository outboxEventRepository;

    @GetMapping("/{advanceId}")
    @PreAuthorize("hasAnyRole('DISPATCHER','FINANCE_OFFICER','FINANCE_DIRECTOR','ADMIN')")
    @Operation(summary = "Get payment status by advance ID")
    public ResponseEntity<PaymentStatusDto> getPaymentStatus(@PathVariable UUID advanceId) {
        return paymentRepository.findByAdvanceId(advanceId)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/outbox/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get outbox event statistics")
    public OutboxStatsDto getOutboxStats() {
        return new OutboxStatsDto(
                outboxEventRepository.countByStatus("PENDING"),
                outboxEventRepository.countByStatus("SENT"),
                outboxEventRepository.countByStatus("FAILED"),
                outboxEventRepository.countByStatus("DEAD_LETTER"),
                outboxEventRepository.findOldestPendingCreatedAt()
        );
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retry a dead-letter outbox event")
    public ResponseEntity<Void> retryDeadLetter(@PathVariable UUID id) {
        outboxEventRepository.findById(id).ifPresent(event -> {
            if ("DEAD_LETTER".equals(event.getStatus())) {
                event.setStatus("PENDING");
                event.setRetryCount(0);
                event.setLastError(null);
                outboxEventRepository.save(event);
            }
        });
        return ResponseEntity.noContent().build();
    }

    private PaymentStatusDto toDto(Payment p) {
        return new PaymentStatusDto(
                p.getId(),
                p.getAdvanceId(),
                p.getStatus(),
                p.getAmount(),
                p.getSbpRequestId(),
                p.getInitiatedAt(),
                p.getCompletedAt(),
                p.getErrorMessage()
        );
    }
}

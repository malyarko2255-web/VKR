package ru.finuniversity.advance.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "advance_id", nullable = false, unique = true)
    private UUID advanceId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private UUID recipientId;

    private String recipientPhone;

    @Column(unique = true)
    private String sbpRequestId;

    private String sbpResponseCode;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Builder.Default
    private Instant initiatedAt = Instant.now();

    private Instant completedAt;
}

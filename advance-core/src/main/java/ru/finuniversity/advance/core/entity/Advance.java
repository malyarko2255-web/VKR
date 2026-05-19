package ru.finuniversity.advance.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.finuniversity.advance.common.dto.AdvanceStatus;
import ru.finuniversity.advance.common.dto.AdvanceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "advances")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Advance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Generated(event = EventType.INSERT)
    @Column(updatable = false, insertable = false)
    private String requestNo;

    @Column(nullable = false)
    private UUID driverId;

    private UUID routeId;
    private UUID tripId;

    @Column(nullable = false)
    private String tripStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdvanceType advanceType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdvanceStatus status = AdvanceStatus.PENDING;

    @Column(precision = 12, scale = 2)
    private BigDecimal limitAvailable;

    @Builder.Default
    private Boolean autoApproved = false;

    private Integer scoreAtCreation;

    private UUID dispatcherId;
    private Instant dispatcherAt;
    @Column(columnDefinition = "TEXT")
    private String dispatcherComment;

    private UUID financeId;
    private Instant financeAt;
    @Column(columnDefinition = "TEXT")
    private String financeComment;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Integer version;

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }
}

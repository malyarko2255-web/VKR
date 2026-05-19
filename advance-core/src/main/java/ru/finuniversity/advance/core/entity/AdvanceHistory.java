package ru.finuniversity.advance.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "advance_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AdvanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advance_id", nullable = false)
    private Advance advance;

    private String fromStatus;

    @Column(nullable = false)
    private String toStatus;

    private UUID changedBy;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Builder.Default
    private Instant changedAt = Instant.now();
}

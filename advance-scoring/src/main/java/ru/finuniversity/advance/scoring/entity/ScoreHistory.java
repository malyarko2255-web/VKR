package ru.finuniversity.advance.scoring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "score_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "schedule_score")
    private Integer scheduleScore;

    @Column(name = "advance_closure_score")
    private Integer advanceClosureScore;

    @Column(name = "fuel_efficiency_score")
    private Integer fuelEfficiencyScore;

    @Column(name = "default_history_score")
    private Integer defaultHistoryScore;

    @Column(name = "tenure_score")
    private Integer tenureScore;

    @Column(name = "calculated_at")
    @Builder.Default
    private Instant calculatedAt = Instant.now();
}

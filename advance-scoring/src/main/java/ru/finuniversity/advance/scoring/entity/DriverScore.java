package ru.finuniversity.advance.scoring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_scores")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DriverScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "driver_id", nullable = false, unique = true)
    private UUID driverId;

    @Column(name = "total_score", nullable = false)
    @Builder.Default
    private Integer totalScore = 50;

    @Column(name = "schedule_score")
    @Builder.Default
    private Integer scheduleScore = 50;

    @Column(name = "advance_closure_score")
    @Builder.Default
    private Integer advanceClosureScore = 50;

    @Column(name = "fuel_efficiency_score")
    @Builder.Default
    private Integer fuelEfficiencyScore = 50;

    @Column(name = "default_history_score")
    @Builder.Default
    private Integer defaultHistoryScore = 50;

    @Column(name = "tenure_score")
    @Builder.Default
    private Integer tenureScore = 50;

    @Column(name = "trips_count")
    @Builder.Default
    private Integer tripsCount = 0;

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    @Version
    private Integer version;
}

package ru.finuniversity.advance.scoring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "telematics_events")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TelematicsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "fuel_consumed_liters")
    private Double fuelConsumedLiters;

    @Column(name = "speeding_minutes")
    @Builder.Default
    private Integer speedingMinutes = 0;

    @Column(name = "harsh_braking_count")
    @Builder.Default
    private Integer harshBrakingCount = 0;

    @Column(name = "on_schedule")
    @Builder.Default
    private Boolean onSchedule = true;

    @Column(name = "trip_stage")
    private String tripStage;

    @Column(name = "recorded_at")
    @Builder.Default
    private Instant recordedAt = Instant.now();
}

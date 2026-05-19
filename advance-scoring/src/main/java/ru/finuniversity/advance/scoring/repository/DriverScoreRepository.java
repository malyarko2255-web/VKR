package ru.finuniversity.advance.scoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.finuniversity.advance.scoring.entity.DriverScore;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DriverScoreRepository extends JpaRepository<DriverScore, UUID> {

    Optional<DriverScore> findByDriverId(UUID driverId);

    @Modifying
    @Query(value = """
        INSERT INTO driver_scores (driver_id, total_score, schedule_score, advance_closure_score,
            fuel_efficiency_score, default_history_score, tenure_score, trips_count, calculated_at, version)
        VALUES (:driverId, :total, :schedule, :closure, :fuel, :defaults, :tenure, :trips, :calculatedAt, 0)
        ON CONFLICT (driver_id) DO UPDATE SET
            total_score = EXCLUDED.total_score,
            schedule_score = EXCLUDED.schedule_score,
            advance_closure_score = EXCLUDED.advance_closure_score,
            fuel_efficiency_score = EXCLUDED.fuel_efficiency_score,
            default_history_score = EXCLUDED.default_history_score,
            tenure_score = EXCLUDED.tenure_score,
            trips_count = EXCLUDED.trips_count,
            calculated_at = EXCLUDED.calculated_at,
            version = driver_scores.version + 1
        """, nativeQuery = true)
    void upsert(@Param("driverId") UUID driverId,
                @Param("total") int total,
                @Param("schedule") int schedule,
                @Param("closure") int closure,
                @Param("fuel") int fuel,
                @Param("defaults") int defaults,
                @Param("tenure") int tenure,
                @Param("trips") int trips,
                @Param("calculatedAt") Instant calculatedAt);
}

package ru.finuniversity.advance.scoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.finuniversity.advance.scoring.entity.TelematicsEvent;

import java.time.Instant;
import java.util.UUID;

public interface TelematicsEventRepository extends JpaRepository<TelematicsEvent, Long> {

    long countByDriverId(UUID driverId);

    @Query("SELECT COALESCE(AVG(e.fuelConsumedLiters / NULLIF(e.distanceKm, 0)), 0) " +
           "FROM TelematicsEvent e WHERE e.driverId = :driverId AND e.occurredAt > :since")
    Double avgFuelPer100km(@Param("driverId") UUID driverId, @Param("since") Instant since);

    @Query("SELECT COUNT(e) FROM TelematicsEvent e WHERE e.driverId = :driverId " +
           "AND e.onSchedule = true AND e.occurredAt > :since")
    long countOnSchedule(@Param("driverId") UUID driverId, @Param("since") Instant since);

    @Query("SELECT COUNT(e) FROM TelematicsEvent e WHERE e.driverId = :driverId " +
           "AND e.occurredAt > :since")
    long countSince(@Param("driverId") UUID driverId, @Param("since") Instant since);
}

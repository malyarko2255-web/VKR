package ru.finuniversity.advance.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.reference.entity.DriverLimit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverLimitRepository extends JpaRepository<DriverLimit, UUID> {

    List<DriverLimit> findByDriverId(UUID driverId);

    Optional<DriverLimit> findByDriverIdAndAdvanceType(UUID driverId, String advanceType);
}

package ru.finuniversity.advance.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.finuniversity.advance.reference.entity.Driver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

    List<Driver> findByUserIdAndActiveTrue(UUID userId);

    @Query("SELECT d FROM Driver d LEFT JOIN FETCH d.limits WHERE d.id = :id")
    Optional<Driver> findByIdWithLimits(@Param("id") UUID id);

    List<Driver> findByContractorIdAndActiveTrue(UUID contractorId);
}

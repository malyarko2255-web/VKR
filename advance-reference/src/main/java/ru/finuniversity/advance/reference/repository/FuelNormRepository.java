package ru.finuniversity.advance.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.reference.entity.FuelNorm;

import java.util.Optional;
import java.util.UUID;

public interface FuelNormRepository extends JpaRepository<FuelNorm, UUID> {

    Optional<FuelNorm> findFirstByVehicleCategoryOrderByEffectiveFromDesc(String vehicleCategory);
}

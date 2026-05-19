package ru.finuniversity.advance.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.reference.entity.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByPlateNo(String plateNo);

    List<Vehicle> findByActiveTrue();
}

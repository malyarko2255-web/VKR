package ru.finuniversity.advance.reference.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.reference.entity.Contractor;

import java.util.Optional;
import java.util.UUID;

public interface ContractorRepository extends JpaRepository<Contractor, UUID> {

    Optional<Contractor> findByInn(String inn);
}

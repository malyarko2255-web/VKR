package ru.finuniversity.advance.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.payment.entity.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByAdvanceId(UUID advanceId);
}

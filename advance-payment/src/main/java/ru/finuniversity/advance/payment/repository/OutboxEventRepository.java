package ru.finuniversity.advance.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.finuniversity.advance.payment.entity.OutboxEvent;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findPendingForUpdate();

    long countByStatus(String status);

    @Query("SELECT MIN(e.createdAt) FROM OutboxEvent e WHERE e.status = 'PENDING'")
    java.time.Instant findOldestPendingCreatedAt();
}

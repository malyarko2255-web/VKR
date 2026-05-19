package ru.finuniversity.advance.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.core.entity.AdvanceHistory;

import java.util.List;
import java.util.UUID;

public interface AdvanceHistoryRepository extends JpaRepository<AdvanceHistory, Long> {

    List<AdvanceHistory> findByAdvanceIdOrderByChangedAtAsc(UUID advanceId);
}

package ru.finuniversity.advance.scoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.finuniversity.advance.scoring.entity.ScoreHistory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, Long> {

    List<ScoreHistory> findByDriverIdAndCalculatedAtAfterOrderByCalculatedAtDesc(UUID driverId, Instant after);
}

package ru.finuniversity.advance.scoring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.finuniversity.advance.common.dto.DriverScoreDto;
import ru.finuniversity.advance.common.dto.TelematicsEventDto;
import ru.finuniversity.advance.scoring.dto.ScoreHistoryDto;
import ru.finuniversity.advance.scoring.entity.DriverScore;
import ru.finuniversity.advance.scoring.entity.ScoreHistory;
import ru.finuniversity.advance.scoring.kafka.TelematicsEventConsumer;
import ru.finuniversity.advance.scoring.repository.DriverScoreRepository;
import ru.finuniversity.advance.scoring.repository.ScoreHistoryRepository;
import ru.finuniversity.advance.scoring.service.ScoringCalculatorService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScoringController {

    private final ScoringCalculatorService scoringCalculatorService;
    private final DriverScoreRepository driverScoreRepository;
    private final ScoreHistoryRepository scoreHistoryRepository;
    private final TelematicsEventConsumer telematicsEventConsumer;

    @GetMapping("/drivers/{id}")
    public ResponseEntity<DriverScoreDto> getScore(@PathVariable UUID id) {
        return driverScoreRepository.findByDriverId(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/drivers/{id}/history")
    public List<ScoreHistoryDto> getHistory(@PathVariable UUID id,
                                             @RequestParam(defaultValue = "30") int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return scoreHistoryRepository
                .findByDriverIdAndCalculatedAtAfterOrderByCalculatedAtDesc(id, since)
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    @PostMapping("/drivers/{id}/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_DIRECTOR', 'DISPATCHER')")
    public DriverScoreDto recalculate(@PathVariable UUID id) {
        return scoringCalculatorService.calculateAndSave(id);
    }

    @PostMapping("/telematics/events")
    public ResponseEntity<Void> ingestEvents(@RequestBody List<TelematicsEventDto> events) {
        telematicsEventConsumer.consume(events);
        return ResponseEntity.accepted().build();
    }

    private DriverScoreDto toDto(DriverScore s) {
        String riskLevel = s.getTotalScore() >= 80 ? "LOW"
                : s.getTotalScore() >= 60 ? "MEDIUM" : "HIGH";
        return new DriverScoreDto(
                s.getDriverId().toString(),
                s.getTotalScore(),
                s.getScheduleScore(),
                s.getAdvanceClosureScore(),
                s.getFuelEfficiencyScore(),
                s.getDefaultHistoryScore(),
                s.getTenureScore(),
                s.getCalculatedAt() != null ? LocalDateTime.ofInstant(s.getCalculatedAt(),
                        java.time.ZoneOffset.UTC) : null,
                riskLevel
        );
    }

    private ScoreHistoryDto toHistoryDto(ScoreHistory h) {
        return new ScoreHistoryDto(
                h.getId(),
                h.getDriverId(),
                h.getTotalScore(),
                h.getScheduleScore(),
                h.getAdvanceClosureScore(),
                h.getFuelEfficiencyScore(),
                h.getDefaultHistoryScore(),
                h.getTenureScore(),
                h.getCalculatedAt()
        );
    }
}

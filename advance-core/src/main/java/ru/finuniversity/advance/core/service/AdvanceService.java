package ru.finuniversity.advance.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.common.dto.*;
import ru.finuniversity.advance.common.events.*;
import ru.finuniversity.advance.common.exception.AdvanceNotFoundException;
import ru.finuniversity.advance.common.exception.UnauthorizedActionException;
import ru.finuniversity.advance.core.client.ReferenceClient;
import ru.finuniversity.advance.core.dto.AvailableLimitDto;
import ru.finuniversity.advance.core.dto.DriverResponseDto;
import ru.finuniversity.advance.core.entity.Advance;
import ru.finuniversity.advance.core.entity.AdvanceHistory;
import ru.finuniversity.advance.core.kafka.CoreEventPublisher;
import ru.finuniversity.advance.core.repository.AdvanceHistoryRepository;
import ru.finuniversity.advance.core.repository.AdvanceRepository;
import ru.finuniversity.advance.core.repository.AdvanceSpecification;
import ru.finuniversity.advance.core.validator.OpenAdvanceValidator;
import ru.finuniversity.advance.core.validator.TripStageValidator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdvanceService {

    private static final String SCORE_KEY_PREFIX = "score:";
    private static final int DEFAULT_SCORE = 50;

    private final AdvanceRepository       advanceRepository;
    private final AdvanceHistoryRepository historyRepository;
    private final LimitService            limitService;
    private final OpenAdvanceValidator    openAdvanceValidator;
    private final TripStageValidator      tripStageValidator;
    private final ReferenceClient         referenceClient;
    private final StringRedisTemplate     redisTemplate;
    private final CoreEventPublisher      eventPublisher;

    public AdvanceResponseDto create(AdvanceRequestDto dto, UUID requesterId) {
        UUID driverId = UUID.fromString(dto.driverId());

        DriverResponseDto driver = referenceClient.getDriver(driverId);
        if (driver == null || !driver.active()) {
            throw new UnauthorizedActionException(driverId.toString(), "create_advance (driver inactive or not found)");
        }

        openAdvanceValidator.validate(driverId, dto.advanceType());

        UUID tripId = dto.tripId() != null ? UUID.fromString(dto.tripId()) : null;
        tripStageValidator.validate(driverId, dto.tripId(), dto.tripStage());

        AvailableLimitDto limitDto = limitService.checkLimit(driverId, dto.advanceType(), dto.amount());

        int score = loadScore(driverId);

        boolean autoApproved = isAutoApproved(score, dto.amount(), limitDto.autoApproveThreshold());
        AdvanceStatus status = autoApproved ? AdvanceStatus.APPROVED : AdvanceStatus.DISPATCHER_REVIEW;

        UUID routeId = dto.routeId() != null ? UUID.fromString(dto.routeId()) : null;

        Advance advance = Advance.builder()
                .driverId(driverId)
                .routeId(routeId)
                .tripId(tripId)
                .tripStage(dto.tripStage())
                .advanceType(dto.advanceType())
                .amount(dto.amount())
                .status(status)
                .limitAvailable(limitDto.available())
                .autoApproved(autoApproved)
                .scoreAtCreation(score)
                .notes(dto.notes())
                .build();

        advance = advanceRepository.saveAndFlush(advance);

        limitService.applyLimit(driverId, dto.advanceType(), dto.amount());

        saveHistory(advance, null, status, requesterId, null);

        eventPublisher.publishAdvanceCreated(new AdvanceCreatedEvent(
                UUID.randomUUID().toString(),
                advance.getId().toString(),
                "ADVANCE_CREATED",
                LocalDateTime.now(),
                driverId.toString(),
                routeId != null ? routeId.toString() : null,
                dto.advanceType(),
                dto.amount(),
                status
        ));

        log.info("Created advance {} for driver {} status={} autoApproved={}",
                advance.getId(), driverId, status, autoApproved);

        return toDto(advance, driver.fullName(), null);
    }

    public AdvanceResponseDto approve(UUID advanceId, ApproveRequestDto dto,
                                       UUID approverId, String approverRole) {
        Advance advance = loadAdvance(advanceId);
        AdvanceStatus currentStatus = advance.getStatus();

        if (currentStatus == AdvanceStatus.DISPATCHER_REVIEW) {
            if (!isDispatcherOrAbove(approverRole)) {
                throw new UnauthorizedActionException(
                        approverId.toString(), "approve at DISPATCHER_REVIEW (role: " + approverRole + ")");
            }
        } else if (currentStatus == AdvanceStatus.FINANCE_REVIEW) {
            if (!isFinanceDirectorOrAdmin(approverRole)) {
                throw new UnauthorizedActionException(
                        approverId.toString(), "approve at FINANCE_REVIEW (role: " + approverRole + ")");
            }
        } else {
            throw new UnauthorizedActionException(
                    approverId.toString(), "approve advance in status " + currentStatus);
        }

        AdvanceStatus newStatus;
        if (currentStatus == AdvanceStatus.DISPATCHER_REVIEW) {
            BigDecimal threshold = getFinanceThreshold(advance.getDriverId(), advance.getAdvanceType());
            newStatus = advance.getAmount().compareTo(threshold) <= 0
                    ? AdvanceStatus.APPROVED
                    : AdvanceStatus.FINANCE_REVIEW;
        } else {
            newStatus = AdvanceStatus.APPROVED;
        }

        if (currentStatus == AdvanceStatus.DISPATCHER_REVIEW) {
            advance.setDispatcherId(approverId);
            advance.setDispatcherAt(Instant.now());
            advance.setDispatcherComment(dto.comment());
        } else {
            advance.setFinanceId(approverId);
            advance.setFinanceAt(Instant.now());
            advance.setFinanceComment(dto.comment());
        }
        advance.setStatus(newStatus);
        advance = advanceRepository.save(advance);

        saveHistory(advance, currentStatus, newStatus, approverId, dto.comment());

        AdvanceApprovedEvent approvedEvent = new AdvanceApprovedEvent(
                UUID.randomUUID().toString(),
                advanceId.toString(),
                "ADVANCE_APPROVED",
                LocalDateTime.now(),
                advance.getDriverId().toString(),
                approverId.toString(),
                dto.comment(),
                newStatus
        );
        eventPublisher.publishAdvanceApproved(approvedEvent);

        if (newStatus == AdvanceStatus.APPROVED) {
            eventPublisher.publishPaymentInitiated(new PaymentInitiatedEvent(
                    UUID.randomUUID().toString(),
                    advanceId.toString(),
                    "PAYMENT_INITIATED",
                    LocalDateTime.now(),
                    advance.getAmount(),
                    advance.getDriverId().toString(),
                    null
            ));
        }

        log.info("Approved advance {} by {} newStatus={}", advanceId, approverId, newStatus);
        return toDto(advance, null, null);
    }

    public AdvanceResponseDto reject(UUID advanceId, RejectRequestDto dto, UUID rejecterId) {
        Advance advance = loadAdvance(advanceId);
        AdvanceStatus current = advance.getStatus();

        if (current == AdvanceStatus.PAID || current == AdvanceStatus.REPORTED
                || current == AdvanceStatus.CLOSED || current == AdvanceStatus.REJECTED) {
            throw new UnauthorizedActionException(rejecterId.toString(), "reject advance in status " + current);
        }

        AdvanceStatus prev = advance.getStatus();
        advance.setStatus(AdvanceStatus.REJECTED);
        advance.setRejectionReason(dto.reason());
        advance = advanceRepository.save(advance);

        limitService.releaseLimit(advance.getDriverId(), advance.getAdvanceType(), advance.getAmount());

        saveHistory(advance, prev, AdvanceStatus.REJECTED, rejecterId, dto.reason());

        eventPublisher.publishAdvanceRejected(new AdvanceRejectedEvent(
                UUID.randomUUID().toString(),
                advanceId.toString(),
                "ADVANCE_REJECTED",
                LocalDateTime.now(),
                advance.getDriverId().toString(),
                rejecterId.toString(),
                dto.reason()
        ));

        log.info("Rejected advance {} by {}", advanceId, rejecterId);
        return toDto(advance, null, null);
    }

    public AdvanceResponseDto report(UUID advanceId, AdvanceReportDto dto, UUID reporterId) {
        Advance advance = loadAdvance(advanceId);
        if (advance.getStatus() != AdvanceStatus.PAID) {
            throw new UnauthorizedActionException(reporterId.toString(),
                    "report advance in status " + advance.getStatus() + " (must be PAID)");
        }
        advance.setStatus(AdvanceStatus.REPORTED);
        advance = advanceRepository.save(advance);
        saveHistory(advance, AdvanceStatus.PAID, AdvanceStatus.REPORTED, reporterId, dto.notes());
        return toDto(advance, null, null);
    }

    @Transactional(readOnly = true)
    public Page<AdvanceResponseDto> getList(AdvanceStatus status, UUID driverId,
                                             AdvanceType advanceType, LocalDate dateFrom,
                                             LocalDate dateTo, UUID requesterId,
                                             String role, Pageable pageable) {
        UUID filterDriverId = isDriverOrContractor(role) ? requesterId : driverId;

        return advanceRepository.findAll(
                AdvanceSpecification.byFilter(status, filterDriverId, advanceType, dateFrom, dateTo),
                pageable
        ).map(a -> toDto(a, null, null));
    }

    @Transactional(readOnly = true)
    public AdvanceResponseDto getById(UUID advanceId) {
        return toDto(loadAdvance(advanceId), null, null);
    }

    @Transactional(readOnly = true)
    public BigDecimal getUsedThisMonth(UUID driverId, AdvanceType advanceType) {
        return advanceRepository.sumAmountByDriverAndTypeAndStatusIn(
                driverId, advanceType,
                java.util.List.of(AdvanceStatus.PENDING, AdvanceStatus.DISPATCHER_REVIEW,
                        AdvanceStatus.FINANCE_REVIEW, AdvanceStatus.APPROVED, AdvanceStatus.PAID));
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Advance loadAdvance(UUID id) {
        return advanceRepository.findById(id)
                .orElseThrow(() -> new AdvanceNotFoundException(id.toString()));
    }

    private int loadScore(UUID driverId) {
        try {
            String cached = redisTemplate.opsForValue().get(SCORE_KEY_PREFIX + driverId);
            if (cached != null) {
                return Integer.parseInt(cached);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for score lookup, using default score={}", DEFAULT_SCORE);
        }
        return DEFAULT_SCORE;
    }

    private boolean isAutoApproved(int score, BigDecimal amount, BigDecimal threshold) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        if (score >= 80 && amount.compareTo(threshold) <= 0) {
            return true;
        }
        BigDecimal reducedThreshold = threshold.multiply(BigDecimal.valueOf(0.6));
        return score >= 60 && amount.compareTo(reducedThreshold) <= 0;
    }

    private BigDecimal getFinanceThreshold(UUID driverId, AdvanceType type) {
        try {
            java.util.List<ru.finuniversity.advance.core.dto.LimitResponseDto> limits =
                    referenceClient.getDriverLimits(driverId);
            return limits.stream()
                    .filter(l -> type.name().equals(l.advanceType()))
                    .map(ru.finuniversity.advance.core.dto.LimitResponseDto::autoApproveThreshold)
                    .findFirst()
                    .orElse(BigDecimal.valueOf(50000));
        } catch (Exception e) {
            return BigDecimal.valueOf(50000);
        }
    }

    private void saveHistory(Advance advance, AdvanceStatus from, AdvanceStatus to,
                              UUID changedBy, String comment) {
        historyRepository.save(AdvanceHistory.builder()
                .advance(advance)
                .fromStatus(from != null ? from.name() : null)
                .toStatus(to.name())
                .changedBy(changedBy)
                .comment(comment)
                .build());
    }

    private boolean isDispatcherOrAbove(String role) {
        return role != null && (role.contains("DISPATCHER") || role.contains("FINANCE_OFFICER")
                || role.contains("FINANCE_DIRECTOR") || role.contains("ADMIN"));
    }

    private boolean isFinanceDirectorOrAdmin(String role) {
        return role != null && (role.contains("FINANCE_DIRECTOR") || role.contains("ADMIN"));
    }

    private boolean isDriverOrContractor(String role) {
        return role != null && (role.contains("DRIVER") || role.contains("CONTRACTOR"));
    }

    private AdvanceResponseDto toDto(Advance a, String driverName, String routeName) {
        return new AdvanceResponseDto(
                a.getId() != null ? a.getId().toString() : null,
                a.getRequestNo(),
                a.getDriverId() != null ? a.getDriverId().toString() : null,
                driverName,
                a.getRouteId() != null ? a.getRouteId().toString() : null,
                routeName,
                a.getTripStage(),
                a.getAdvanceType(),
                a.getAmount(),
                a.getLimitAvailable(),
                a.getStatus(),
                a.getAutoApproved(),
                a.getScoreAtCreation(),
                a.getDispatcherComment(),
                a.getFinanceComment(),
                a.getRejectionReason(),
                a.getCreatedAt() != null ? LocalDateTime.ofInstant(a.getCreatedAt(), ZoneOffset.UTC) : null,
                a.getUpdatedAt() != null ? LocalDateTime.ofInstant(a.getUpdatedAt(), ZoneOffset.UTC) : null
        );
    }
}

package ru.finuniversity.advance.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.finuniversity.advance.common.dto.*;
import ru.finuniversity.advance.common.exception.LimitExceededException;
import ru.finuniversity.advance.common.exception.OpenAdvanceExistsException;
import ru.finuniversity.advance.common.exception.UnauthorizedActionException;
import ru.finuniversity.advance.core.client.ReferenceClient;
import ru.finuniversity.advance.core.dto.AvailableLimitDto;
import ru.finuniversity.advance.core.dto.DriverResponseDto;
import ru.finuniversity.advance.core.entity.Advance;
import ru.finuniversity.advance.core.entity.AdvanceHistory;
import ru.finuniversity.advance.core.kafka.CoreEventPublisher;
import ru.finuniversity.advance.core.repository.AdvanceHistoryRepository;
import ru.finuniversity.advance.core.repository.AdvanceRepository;
import ru.finuniversity.advance.core.validator.OpenAdvanceValidator;
import ru.finuniversity.advance.core.validator.TripStageValidator;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvanceServiceTest {

    @Mock AdvanceRepository       advanceRepository;
    @Mock AdvanceHistoryRepository historyRepository;
    @Mock LimitService            limitService;
    @Mock OpenAdvanceValidator    openAdvanceValidator;
    @Mock TripStageValidator      tripStageValidator;
    @Mock ReferenceClient         referenceClient;
    @Mock StringRedisTemplate     redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock CoreEventPublisher      eventPublisher;

    @InjectMocks AdvanceService advanceService;

    private static final UUID DRIVER_ID   = UUID.fromString("d1000000-0000-0000-0000-000000000001");
    private static final UUID REQUESTER   = UUID.randomUUID();
    private static final UUID APPROVER    = UUID.randomUUID();
    private static final UUID ADVANCE_ID  = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void create_autoApproved_whenHighScoreAndLowAmount() {
        AdvanceRequestDto dto = new AdvanceRequestDto(
                DRIVER_ID.toString(), null, null, "LOADING",
                AdvanceType.FUEL, BigDecimal.valueOf(4000), null);

        when(referenceClient.getDriver(DRIVER_ID))
                .thenReturn(new DriverResponseDto(DRIVER_ID, null, "Иван", null, null, null, null, true));
        when(valueOps.get("score:" + DRIVER_ID)).thenReturn("85");

        AvailableLimitDto limitDto = new AvailableLimitDto(
                BigDecimal.valueOf(50000), BigDecimal.valueOf(10000),
                BigDecimal.valueOf(40000), BigDecimal.valueOf(8000),
                BigDecimal.valueOf(4000), true);
        when(limitService.checkLimit(DRIVER_ID, AdvanceType.FUEL, BigDecimal.valueOf(4000)))
                .thenReturn(limitDto);

        Advance saved = Advance.builder()
                .id(ADVANCE_ID).driverId(DRIVER_ID)
                .advanceType(AdvanceType.FUEL).amount(BigDecimal.valueOf(4000))
                .status(AdvanceStatus.APPROVED).autoApproved(true).scoreAtCreation(85)
                .build();
        when(advanceRepository.saveAndFlush(any(Advance.class))).thenReturn(saved);
        when(historyRepository.save(any(AdvanceHistory.class))).thenReturn(null);

        AdvanceResponseDto result = advanceService.create(dto, REQUESTER);

        assertThat(result.status()).isEqualTo(AdvanceStatus.APPROVED);
        assertThat(result.autoApproved()).isTrue();
        verify(limitService).applyLimit(DRIVER_ID, AdvanceType.FUEL, BigDecimal.valueOf(4000));
        verify(eventPublisher).publishAdvanceCreated(any());
    }

    @Test
    void create_requiresDispatcher_whenScoreInsufficient() {
        AdvanceRequestDto dto = new AdvanceRequestDto(
                DRIVER_ID.toString(), null, null, "IN_TRANSIT",
                AdvanceType.FUEL, BigDecimal.valueOf(6000), null);

        when(referenceClient.getDriver(DRIVER_ID))
                .thenReturn(new DriverResponseDto(DRIVER_ID, null, "Пётр", null, null, null, null, true));
        when(valueOps.get("score:" + DRIVER_ID)).thenReturn("55");

        AvailableLimitDto limitDto = new AvailableLimitDto(
                BigDecimal.valueOf(50000), BigDecimal.ZERO,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(8000),
                BigDecimal.valueOf(6000), true);
        when(limitService.checkLimit(DRIVER_ID, AdvanceType.FUEL, BigDecimal.valueOf(6000)))
                .thenReturn(limitDto);

        Advance saved = Advance.builder()
                .id(ADVANCE_ID).driverId(DRIVER_ID)
                .advanceType(AdvanceType.FUEL).amount(BigDecimal.valueOf(6000))
                .status(AdvanceStatus.DISPATCHER_REVIEW).autoApproved(false).scoreAtCreation(55)
                .build();
        when(advanceRepository.saveAndFlush(any(Advance.class))).thenReturn(saved);
        when(historyRepository.save(any(AdvanceHistory.class))).thenReturn(null);

        AdvanceResponseDto result = advanceService.create(dto, REQUESTER);

        assertThat(result.status()).isEqualTo(AdvanceStatus.DISPATCHER_REVIEW);
        assertThat(result.autoApproved()).isFalse();
    }

    @Test
    void create_throwsLimitExceeded_whenOverLimit() {
        AdvanceRequestDto dto = new AdvanceRequestDto(
                DRIVER_ID.toString(), null, null, "LOADING",
                AdvanceType.FUEL, BigDecimal.valueOf(60000), null);

        when(referenceClient.getDriver(DRIVER_ID))
                .thenReturn(new DriverResponseDto(DRIVER_ID, null, "Иван", null, null, null, null, true));
        when(limitService.checkLimit(eq(DRIVER_ID), eq(AdvanceType.FUEL), any()))
                .thenThrow(new LimitExceededException(DRIVER_ID.toString(), AdvanceType.FUEL,
                        BigDecimal.valueOf(60000), BigDecimal.valueOf(40000)));

        assertThatThrownBy(() -> advanceService.create(dto, REQUESTER))
                .isInstanceOf(LimitExceededException.class);

        verify(advanceRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_throwsOpenAdvance_whenExistingNotClosed() {
        AdvanceRequestDto dto = new AdvanceRequestDto(
                DRIVER_ID.toString(), null, null, "LOADING",
                AdvanceType.FUEL, BigDecimal.valueOf(5000), null);

        when(referenceClient.getDriver(DRIVER_ID))
                .thenReturn(new DriverResponseDto(DRIVER_ID, null, "Сидор", null, null, null, null, true));
        doThrow(new OpenAdvanceExistsException(DRIVER_ID.toString(), UUID.randomUUID().toString()))
                .when(openAdvanceValidator).validate(DRIVER_ID, AdvanceType.FUEL);

        assertThatThrownBy(() -> advanceService.create(dto, REQUESTER))
                .isInstanceOf(OpenAdvanceExistsException.class);
    }

    @Test
    void approve_movesToFinanceReview_whenAmountAboveThreshold() {
        Advance advance = Advance.builder()
                .id(ADVANCE_ID).driverId(DRIVER_ID)
                .advanceType(AdvanceType.FUEL).amount(BigDecimal.valueOf(80000))
                .status(AdvanceStatus.DISPATCHER_REVIEW).version(0)
                .build();

        when(advanceRepository.findById(ADVANCE_ID)).thenReturn(Optional.of(advance));
        when(referenceClient.getDriverLimits(DRIVER_ID)).thenReturn(
                java.util.List.of(new ru.finuniversity.advance.core.dto.LimitResponseDto(
                        null, "FUEL", BigDecimal.valueOf(50000), null, BigDecimal.valueOf(5000))));
        when(advanceRepository.save(any(Advance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any(AdvanceHistory.class))).thenReturn(null);

        ApproveRequestDto dto = new ApproveRequestDto("Approved by dispatcher");
        AdvanceResponseDto result = advanceService.approve(ADVANCE_ID, dto, APPROVER, "DISPATCHER");

        assertThat(result.status()).isEqualTo(AdvanceStatus.FINANCE_REVIEW);
        verify(eventPublisher).publishAdvanceApproved(any());
        verify(eventPublisher, never()).publishPaymentInitiated(any());
    }

    @Test
    void approve_movesToApproved_whenFinanceDirectorApproves() {
        Advance advance = Advance.builder()
                .id(ADVANCE_ID).driverId(DRIVER_ID)
                .advanceType(AdvanceType.FUEL).amount(BigDecimal.valueOf(80000))
                .status(AdvanceStatus.FINANCE_REVIEW).version(0)
                .build();

        when(advanceRepository.findById(ADVANCE_ID)).thenReturn(Optional.of(advance));
        when(advanceRepository.save(any(Advance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any(AdvanceHistory.class))).thenReturn(null);

        ApproveRequestDto dto = new ApproveRequestDto("Approved by finance director");
        AdvanceResponseDto result = advanceService.approve(ADVANCE_ID, dto, APPROVER, "FINANCE_DIRECTOR");

        assertThat(result.status()).isEqualTo(AdvanceStatus.APPROVED);
        verify(eventPublisher).publishPaymentInitiated(any());
    }

    @Test
    void approve_throwsUnauthorized_whenWrongRole() {
        Advance advance = Advance.builder()
                .id(ADVANCE_ID).driverId(DRIVER_ID)
                .status(AdvanceStatus.FINANCE_REVIEW).version(0)
                .build();

        when(advanceRepository.findById(ADVANCE_ID)).thenReturn(Optional.of(advance));

        assertThatThrownBy(() ->
                advanceService.approve(ADVANCE_ID, new ApproveRequestDto(""), APPROVER, "DISPATCHER"))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void reject_releasesLimit() {
        Advance advance = Advance.builder()
                .id(ADVANCE_ID).driverId(DRIVER_ID)
                .advanceType(AdvanceType.FUEL).amount(BigDecimal.valueOf(10000))
                .status(AdvanceStatus.DISPATCHER_REVIEW).version(0)
                .build();

        when(advanceRepository.findById(ADVANCE_ID)).thenReturn(Optional.of(advance));
        when(advanceRepository.save(any(Advance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any(AdvanceHistory.class))).thenReturn(null);

        RejectRequestDto dto = new RejectRequestDto("Недостаточно обоснований");
        advanceService.reject(ADVANCE_ID, dto, APPROVER);

        verify(limitService).releaseLimit(DRIVER_ID, AdvanceType.FUEL, BigDecimal.valueOf(10000));
        verify(eventPublisher).publishAdvanceRejected(any());
    }
}

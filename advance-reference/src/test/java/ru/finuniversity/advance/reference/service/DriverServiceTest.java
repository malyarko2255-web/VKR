package ru.finuniversity.advance.reference.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.finuniversity.advance.reference.dto.DriverDto;
import ru.finuniversity.advance.reference.entity.Driver;
import ru.finuniversity.advance.reference.event.ReferenceEventPublisher;
import ru.finuniversity.advance.reference.exception.DriverNotFoundException;
import ru.finuniversity.advance.reference.repository.DriverLimitRepository;
import ru.finuniversity.advance.reference.repository.DriverRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock DriverRepository       driverRepository;
    @Mock DriverLimitRepository  driverLimitRepository;
    @Mock StringRedisTemplate    redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @Mock ReferenceEventPublisher eventPublisher;

    @InjectMocks DriverService driverService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void getDriver_cacheHit_returnsWithoutDbCall() {
        UUID driverId = UUID.randomUUID();
        String cached = driverId + "|Иван Водитель|true";
        when(valueOps.get("driver:" + driverId)).thenReturn(cached);

        DriverDto dto = driverService.getDriver(driverId);

        assertThat(dto.id()).isEqualTo(driverId);
        assertThat(dto.fullName()).isEqualTo("Иван Водитель");
        assertThat(dto.active()).isTrue();
        verifyNoInteractions(driverRepository);
    }

    @Test
    void getDriver_cacheMiss_loadsFromDb() {
        UUID driverId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn(null);

        Driver driver = Driver.builder()
                .id(driverId)
                .fullName("Пётр Петров")
                .active(true)
                .build();
        when(driverRepository.findByIdWithLimits(driverId)).thenReturn(Optional.of(driver));

        DriverDto dto = driverService.getDriver(driverId);

        assertThat(dto.fullName()).isEqualTo("Пётр Петров");
        verify(driverRepository).findByIdWithLimits(driverId);
        verify(valueOps).set(anyString(), anyString(), any());
    }

    @Test
    void getDriver_notFound_throwsException() {
        UUID driverId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn(null);
        when(driverRepository.findByIdWithLimits(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getDriver(driverId))
                .isInstanceOf(DriverNotFoundException.class);
    }
}

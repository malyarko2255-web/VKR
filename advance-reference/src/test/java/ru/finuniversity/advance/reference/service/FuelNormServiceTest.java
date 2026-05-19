package ru.finuniversity.advance.reference.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.finuniversity.advance.reference.dto.FuelNormResponseDto;
import ru.finuniversity.advance.reference.entity.FuelNorm;
import ru.finuniversity.advance.reference.entity.Route;
import ru.finuniversity.advance.reference.entity.Vehicle;
import ru.finuniversity.advance.reference.repository.FuelNormRepository;
import ru.finuniversity.advance.reference.repository.RouteRepository;
import ru.finuniversity.advance.reference.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelNormServiceTest {

    @Mock VehicleRepository  vehicleRepository;
    @Mock RouteRepository    routeRepository;
    @Mock FuelNormRepository fuelNormRepository;

    @InjectMocks FuelNormService fuelNormService;

    @Test
    void calculate_winterCoefficient_appliesTenPercent() {
        UUID vehicleId = UUID.randomUUID();
        UUID routeId   = UUID.randomUUID();

        Vehicle vehicle = Vehicle.builder().id(vehicleId).category("TRUCK_MEDIUM").build();
        Route   route   = Route.builder().id(routeId).distanceKm(BigDecimal.valueOf(100)).build();
        FuelNorm norm   = FuelNorm.builder()
                .vehicleCategory("TRUCK_MEDIUM")
                .baseNorm(BigDecimal.valueOf(28.5))
                .effectiveFrom(LocalDate.now())
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(fuelNormRepository.findFirstByVehicleCategoryOrderByEffectiveFromDesc("TRUCK_MEDIUM"))
                .thenReturn(Optional.of(norm));

        FuelNormResponseDto result = fuelNormService.calculateAdvanceAmount(vehicleId, routeId, "WINTER");

        // WINTER +10%, distance=100km: 28.5 * 1.10 = 31.35 liters
        assertThat(result.adjustedLiters()).isEqualByComparingTo(new BigDecimal("31.35"));
        assertThat(result.season()).isEqualTo("WINTER");
    }

    @Test
    void calculate_summerNoCoefficient_baseNorm() {
        UUID vehicleId = UUID.randomUUID();
        UUID routeId   = UUID.randomUUID();

        Vehicle vehicle = Vehicle.builder().id(vehicleId).category("VAN").build();
        Route   route   = Route.builder().id(routeId).distanceKm(BigDecimal.valueOf(50)).build();
        FuelNorm norm   = FuelNorm.builder()
                .vehicleCategory("VAN")
                .baseNorm(BigDecimal.valueOf(12.0))
                .effectiveFrom(LocalDate.now())
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(fuelNormRepository.findFirstByVehicleCategoryOrderByEffectiveFromDesc("VAN"))
                .thenReturn(Optional.of(norm));

        FuelNormResponseDto result = fuelNormService.calculateAdvanceAmount(vehicleId, routeId, "SUMMER");

        // SUMMER, 50km (<=100, no extra): 12.0 L/100km * 50km = 6.0 liters
        assertThat(result.adjustedLiters()).isEqualByComparingTo(new BigDecimal("6.00"));
    }
}

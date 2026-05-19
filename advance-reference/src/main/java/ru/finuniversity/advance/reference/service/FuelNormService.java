package ru.finuniversity.advance.reference.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.finuniversity.advance.common.util.FuelNormCalculator;
import ru.finuniversity.advance.reference.dto.FuelNormResponseDto;
import ru.finuniversity.advance.reference.entity.FuelNorm;
import ru.finuniversity.advance.reference.entity.Route;
import ru.finuniversity.advance.reference.entity.Vehicle;
import ru.finuniversity.advance.reference.repository.FuelNormRepository;
import ru.finuniversity.advance.reference.repository.RouteRepository;
import ru.finuniversity.advance.reference.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuelNormService {

    private static final BigDecimal FUEL_PRICE_PER_LITER = BigDecimal.valueOf(60.0);

    private final VehicleRepository vehicleRepository;
    private final RouteRepository   routeRepository;
    private final FuelNormRepository fuelNormRepository;

    @Transactional(readOnly = true)
    public FuelNormResponseDto calculateAdvanceAmount(UUID vehicleId, UUID routeId, String season) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + vehicleId));

        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + routeId));

        String category = vehicle.getCategory() != null ? vehicle.getCategory() : "TRUCK_MEDIUM";

        FuelNorm norm = fuelNormRepository
                .findFirstByVehicleCategoryOrderByEffectiveFromDesc(category)
                .orElseThrow(() -> new IllegalStateException("No fuel norm for category: " + category));

        double distanceKm = route.getDistanceKm() != null ? route.getDistanceKm().doubleValue() : 0;
        BigDecimal adjustedLiters = FuelNormCalculator.calculate(
                distanceKm,
                norm.getBaseNorm().doubleValue(),
                season,
                null
        );

        BigDecimal estimatedAmount = adjustedLiters.multiply(FUEL_PRICE_PER_LITER)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Fuel calc: vehicle={} route={} season={} liters={} amount={}",
                vehicleId, routeId, season, adjustedLiters, estimatedAmount);

        return new FuelNormResponseDto(
                category,
                norm.getBaseNorm(),
                route.getDistanceKm(),
                season,
                adjustedLiters,
                estimatedAmount
        );
    }
}

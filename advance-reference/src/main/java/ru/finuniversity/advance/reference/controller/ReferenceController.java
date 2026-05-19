package ru.finuniversity.advance.reference.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.finuniversity.advance.common.dto.DriverLimitDto;
import ru.finuniversity.advance.reference.dto.DriverDto;
import ru.finuniversity.advance.reference.dto.FuelNormResponseDto;
import ru.finuniversity.advance.reference.dto.LimitUpdateRequest;
import ru.finuniversity.advance.reference.dto.TripDto;
import ru.finuniversity.advance.reference.entity.DriverLimit;
import ru.finuniversity.advance.reference.repository.DriverLimitRepository;
import ru.finuniversity.advance.reference.repository.RouteRepository;
import ru.finuniversity.advance.reference.repository.VehicleRepository;
import ru.finuniversity.advance.reference.service.DriverService;
import ru.finuniversity.advance.reference.service.FuelNormService;
import ru.finuniversity.advance.reference.client.KisClient;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reference Data", description = "Driver, vehicle, route and limit reference data")
public class ReferenceController {

    private final DriverService         driverService;
    private final FuelNormService       fuelNormService;
    private final DriverLimitRepository driverLimitRepository;
    private final RouteRepository       routeRepository;
    private final VehicleRepository     vehicleRepository;
    private final KisClient             kisClient;

    @GetMapping("/drivers/{id}")
    @Operation(summary = "Get driver by ID")
    public DriverDto getDriver(@PathVariable UUID id) {
        return driverService.getDriver(id);
    }

    @GetMapping("/drivers/{id}/limits")
    @Operation(summary = "Get driver limits")
    public List<DriverLimit> getDriverLimits(@PathVariable UUID id) {
        return driverLimitRepository.findByDriverId(id);
    }

    @PutMapping("/drivers/{id}/limits")
    @PreAuthorize("hasAnyRole('DISPATCHER','FINANCE_OFFICER','FINANCE_DIRECTOR','ADMIN')")
    @Operation(summary = "Update driver limit")
    public ResponseEntity<Void> updateLimit(
            @PathVariable UUID id,
            @Valid @RequestBody LimitUpdateRequest request) {
        driverService.updateLimit(id, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/drivers/{id}/trips")
    @Operation(summary = "Get active trips for driver from KIS")
    public List<TripDto> getDriverTrips(@PathVariable UUID id) {
        return kisClient.getActiveTrips(id);
    }

    @GetMapping("/fuel-norm/calculate")
    @Operation(summary = "Calculate fuel advance for a trip")
    public FuelNormResponseDto calculateFuelNorm(
            @RequestParam UUID vehicleId,
            @RequestParam UUID routeId,
            @RequestParam(defaultValue = "SUMMER") String season) {
        return fuelNormService.calculateAdvanceAmount(vehicleId, routeId, season);
    }

    @GetMapping("/routes")
    @Operation(summary = "List active routes")
    public List<?> getActiveRoutes() {
        return routeRepository.findByActiveTrue();
    }
}

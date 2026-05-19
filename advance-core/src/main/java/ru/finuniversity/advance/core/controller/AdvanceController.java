package ru.finuniversity.advance.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.finuniversity.advance.common.dto.*;
import ru.finuniversity.advance.core.service.AdvanceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/advances")
@RequiredArgsConstructor
@Tag(name = "Advances", description = "Advance request lifecycle management")
public class AdvanceController {

    private final AdvanceService advanceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DRIVER','CONTRACTOR','DISPATCHER','ADMIN')")
    @Operation(summary = "Create advance request")
    public AdvanceResponseDto create(@Valid @RequestBody AdvanceRequestDto dto,
                                      @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        return advanceService.create(dto, requesterId);
    }

    @GetMapping
    @Operation(summary = "List advance requests")
    public Page<AdvanceResponseDto> list(
            @RequestParam(required = false) AdvanceStatus status,
            @RequestParam(required = false) UUID driverId,
            @RequestParam(required = false) AdvanceType advanceType,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        UUID requesterId = UUID.fromString(jwt.getSubject());
        String role = extractPrimaryRole(jwt);
        return advanceService.getList(status, driverId, advanceType, dateFrom, dateTo,
                requesterId, role, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get advance by ID")
    public AdvanceResponseDto getById(@PathVariable UUID id) {
        return advanceService.getById(id);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('DISPATCHER','FINANCE_OFFICER','FINANCE_DIRECTOR','ADMIN')")
    @Operation(summary = "Approve advance")
    public AdvanceResponseDto approve(@PathVariable UUID id,
                                       @Valid @RequestBody ApproveRequestDto dto,
                                       @AuthenticationPrincipal Jwt jwt) {
        UUID approverId = UUID.fromString(jwt.getSubject());
        String role = extractPrimaryRole(jwt);
        return advanceService.approve(id, dto, approverId, role);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('DISPATCHER','FINANCE_DIRECTOR','ADMIN')")
    @Operation(summary = "Reject advance")
    public AdvanceResponseDto reject(@PathVariable UUID id,
                                      @Valid @RequestBody RejectRequestDto dto,
                                      @AuthenticationPrincipal Jwt jwt) {
        UUID rejecterId = UUID.fromString(jwt.getSubject());
        return advanceService.reject(id, dto, rejecterId);
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('DRIVER','CONTRACTOR')")
    @Operation(summary = "Submit advance expense report")
    public AdvanceResponseDto report(@PathVariable UUID id,
                                      @Valid @RequestBody AdvanceReportDto dto,
                                      @AuthenticationPrincipal Jwt jwt) {
        UUID reporterId = UUID.fromString(jwt.getSubject());
        return advanceService.report(id, dto, reporterId);
    }

    @GetMapping("/used")
    @Operation(summary = "Get used advance amount this month (for Reference Service)")
    public BigDecimal getUsed(@RequestParam UUID driverId,
                               @RequestParam AdvanceType advanceType) {
        return advanceService.getUsedThisMonth(driverId, advanceType);
    }

    private String extractPrimaryRole(Jwt jwt) {
        java.util.List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && !roles.isEmpty() ? roles.get(0) : "";
    }
}

package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.analytics.DriverRestAnalysisDto;
import ru.maslov.trucknavigator.dto.driver.DriverDetailDto;
import ru.maslov.trucknavigator.dto.driver.DriverMedicalDto;
import ru.maslov.trucknavigator.dto.driver.DriverPerformanceDto;
import ru.maslov.trucknavigator.dto.driver.DriverSummaryDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.service.DriverService;
import ru.maslov.trucknavigator.service.RouteService;
import ru.maslov.trucknavigator.service.analytics.DriverPerformanceService;
import ru.maslov.trucknavigator.service.analytics.DriverRestAnalysisService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Водители", description = "API для управления водителями и анализа их данных")
public class DriverController {

    private final DriverService driverService;
    private final RouteService routeService;
    private final DriverRestAnalysisService driverRestService;
    private final DriverPerformanceService performanceService;

    @GetMapping
    @Operation(summary = "Получить всех водителей", description = "Возвращает список всех водителей")
    @PreAuthorize("hasAnyRole('DRIVER', 'DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<DriverSummaryDto>> getAllDrivers() {
        return ResponseEntity.ok(driverService.findAllSummaries());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить водителя по ID", description = "Возвращает детальную информацию о водителе")
    @PreAuthorize("hasAnyRole('DRIVER', 'DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<DriverDetailDto> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.findDetailById(id));
    }

    @PostMapping
    @Operation(summary = "Создать водителя", description = "Создает нового водителя на основе переданных данных")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<DriverDetailDto> createDriver(@Valid @RequestBody Driver driver) {
        return ResponseEntity.ok(driverService.saveAndGetDto(driver));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить водителя", description = "Обновляет существующего водителя")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<DriverDetailDto> updateDriver(
            @PathVariable Long id, @Valid @RequestBody Driver driver) {

        if (!driverService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        driver.setId(id);
        return ResponseEntity.ok(driverService.saveAndGetDto(driver));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить водителя", description = "Удаляет водителя по указанному ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id) {
        driverService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Эндпоинты для режима труда и отдыха (РТО)

    @GetMapping("/{driverId}/rest-analysis/route/{routeId}")
    @Operation(summary = "Анализ РТО для маршрута",
            description = "Анализирует режим труда и отдыха водителя для указанного маршрута")
    @PreAuthorize("hasAnyRole('DRIVER', 'DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<DriverRestAnalysisDto> analyzeDriverRestTime(
            @PathVariable Long driverId,
            @PathVariable Long routeId) {

        Driver driver = driverService.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        Route route = routeService.findById(routeId)
                .orElseThrow(() -> new EntityNotFoundException("Route", routeId));

        DriverRestAnalysisDto analysis = driverRestService.analyzeDriverRestTime(driver, route);

        return ResponseEntity.ok(analysis);
    }

    @PutMapping("/{driverId}/status")
    @Operation(summary = "Обновить статус водителя",
            description = "Обновляет статус водителя в системе контроля РТО")
    @PreAuthorize("hasAnyRole('DRIVER', 'DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<DriverDetailDto> updateDriverStatus(
            @PathVariable Long driverId,
            @RequestParam Driver.DrivingStatus status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp) {

        return ResponseEntity.ok(driverService.updateDriverStatus(driverId, status, timestamp));
    }

    // Эндпоинты для медицинских данных

    @GetMapping("/{driverId}/medical")
    @Operation(summary = "Получить медицинские данные",
            description = "Возвращает информацию о медицинских показателях водителя")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'MEDICAL_STAFF')")
    public ResponseEntity<DriverMedicalDto> getDriverMedical(@PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getDriverMedical(driverId));
    }

    @PutMapping("/{driverId}/medical")
    @Operation(summary = "Обновить медицинские данные",
            description = "Обновляет информацию о медицинских показателях водителя")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'MEDICAL_STAFF')")
    public ResponseEntity<DriverMedicalDto> updateDriverMedical(
            @PathVariable Long driverId,
            @Valid @RequestBody DriverMedicalDto medicalDto) {

        return ResponseEntity.ok(driverService.updateDriverMedical(driverId, medicalDto));
    }

    // Эндпоинты для квалификации

    @GetMapping("/{driverId}/qualifications")
    @Operation(summary = "Получить квалификации водителя",
            description = "Возвращает информацию о квалификациях и допусках водителя")
    @PreAuthorize("hasAnyRole('DRIVER', 'DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Set<String>> getDriverQualifications(@PathVariable Long driverId) {
        return ResponseEntity.ok(driverService.getDriverQualifications(driverId));
    }

    @PutMapping("/{driverId}/qualifications")
    @Operation(summary = "Обновить квалификации водителя",
            description = "Обновляет информацию о квалификациях и допусках водителя")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Set<String>> updateDriverQualifications(
            @PathVariable Long driverId,
            @RequestBody Set<String> qualifications) {

        return ResponseEntity.ok(driverService.updateDriverQualifications(driverId, qualifications));
    }

    // Эндпоинты для эффективности

    @GetMapping("/{driverId}/performance")
    @Operation(summary = "Анализ эффективности водителя",
            description = "Возвращает показатели эффективности работы водителя")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<DriverPerformanceDto> getDriverPerformance(
            @PathVariable Long driverId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDateTime endDate) {

        return ResponseEntity.ok(performanceService.analyzeDriverPerformance(driverId, startDate, endDate));
    }
}
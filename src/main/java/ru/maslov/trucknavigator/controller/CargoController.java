package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.assignment.CargoAssignmentDto;
import ru.maslov.trucknavigator.dto.cargo.CargoDetailDto;
import ru.maslov.trucknavigator.dto.cargo.CargoSummaryDto;
import ru.maslov.trucknavigator.dto.compliance.ComplianceResultDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.service.CargoService;
import ru.maslov.trucknavigator.service.DriverService;
import ru.maslov.trucknavigator.service.RouteService;
import ru.maslov.trucknavigator.service.VehicleService;
import ru.maslov.trucknavigator.service.compliance.CargoComplianceService;

import java.util.List;

@RestController
@RequestMapping("/api/cargos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Грузы", description = "API для управления грузами и их назначения")
public class CargoController {

    private final CargoService cargoService;
    private final RouteService routeService;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final CargoComplianceService complianceService;

    @GetMapping
    @Operation(summary = "Получить все грузы", description = "Возвращает список всех грузов")
    @PreAuthorize("hasAnyRole('DRIVER', 'DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CargoSummaryDto>> getAllCargos() {
        return ResponseEntity.ok(cargoService.findAllSummaries());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить груз по ID", description = "Возвращает детальную информацию о грузе")
    @PreAuthorize("hasAnyRole('DRIVER', 'DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CargoDetailDto> getCargoById(@PathVariable Long id) {
        return ResponseEntity.ok(cargoService.findDetailById(id));
    }

    @PostMapping
    @Operation(summary = "Создать груз", description = "Создает новый груз на основе переданных данных")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CargoDetailDto> createCargo(@Valid @RequestBody Cargo cargo) {
        return ResponseEntity.ok(cargoService.saveAndGetDto(cargo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить груз", description = "Обновляет существующий груз")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CargoDetailDto> updateCargo(
            @PathVariable Long id, @Valid @RequestBody Cargo cargo) {

        if (!cargoService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        cargo.setId(id);
        return ResponseEntity.ok(cargoService.saveAndGetDto(cargo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить груз", description = "Удаляет груз по указанному ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCargo(@PathVariable Long id) {
        cargoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Новые эндпоинты для назначения грузов

    @PostMapping("/{cargoId}/assign")
    @Operation(summary = "Назначить груз",
            description = "Назначает груз для перевозки по маршруту определенным ТС и водителем")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CargoAssignmentDto> assignCargo(
            @PathVariable Long cargoId,
            @RequestParam Long routeId,
            @RequestParam Long vehicleId,
            @RequestParam Long driverId) {

        Cargo cargo = cargoService.findById(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("Груз не найден"));

        Route route = routeService.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Маршрут не найден"));

        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("ТС не найдено"));

        Driver driver = driverService.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Водитель не найден"));

        // Обновляем маршрут с новыми данными
        route.setCargo(cargo);
        route.setVehicle(vehicle);
        route.setDriver(driver);

        Route updatedRoute = routeService.save(route);

        // Формируем ответ
        return ResponseEntity.ok(
                CargoAssignmentDto.builder()
                        .cargoId(cargo.getId())
                        .routeId(route.getId())
                        .vehicleId(vehicle.getId())
                        .driverId(driver.getId())
                        .assignmentDate(updatedRoute.getUpdatedAt())
                        .build()
        );
    }

    @GetMapping("/{cargoId}/compliance/vehicle/{vehicleId}")
    @Operation(summary = "Проверить совместимость груза с ТС",
            description = "Проверяет возможность перевозки груза указанным ТС")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ComplianceResultDto> checkVehicleCompliance(
            @PathVariable Long cargoId,
            @PathVariable Long vehicleId) {

        Cargo cargo = cargoService.findById(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("Груз не найден"));

        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("ТС не найдено"));

        ComplianceResultDto result = complianceService.checkCargoVehicleCompliance(cargo, vehicle);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{cargoId}/compliance/driver/{driverId}")
    @Operation(summary = "Проверить квалификацию водителя для груза",
            description = "Проверяет наличие у водителя необходимых допусков для перевозки груза")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ComplianceResultDto> checkDriverCompliance(
            @PathVariable Long cargoId,
            @PathVariable Long driverId) {

        Cargo cargo = cargoService.findById(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("Груз не найден"));

        Driver driver = driverService.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Водитель не найден"));

        ComplianceResultDto result = complianceService.checkCargoDriverCompliance(cargo, driver);

        return ResponseEntity.ok(result);
    }
}
package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.service.VehicleService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Контроллер для работы с транспортными средствами.
 * Предоставляет API для создания, получения и обновления данных ТС.
 */
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Транспортные средства", description = "API для работы с данными транспортных средств")
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Получение списка всех транспортных средств.
     *
     * @return список ТС
     */
    @GetMapping
    @Operation(summary = "Получить все ТС", description = "Возвращает список всех транспортных средств")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleService.findAll());
    }

    /**
     * Получение транспортного средства по ID.
     *
     * @param id идентификатор ТС
     * @return транспортное средство
     */
    @GetMapping("/{id}")
    @Operation(summary = "Получить ТС по ID", description = "Возвращает транспортное средство по указанному идентификатору")
    public ResponseEntity<Vehicle> getVehicleById(
            @Parameter(description = "Идентификатор ТС") @PathVariable Long id) {
        return vehicleService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Создание нового транспортного средства.
     *
     * @param vehicle данные нового ТС
     * @return созданное ТС
     */
    @PostMapping
    @Operation(summary = "Создать ТС", description = "Создает новое транспортное средство на основе переданных данных")
    public ResponseEntity<Vehicle> createVehicle(
            @Parameter(description = "Данные ТС")
            @Valid @RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.save(vehicle));
    }

    /**
     * Обновление транспортного средства.
     *
     * @param id идентификатор ТС
     * @param vehicle обновленные данные ТС
     * @return обновленное ТС
     */
    @PutMapping("/{id}")
    @Operation(summary = "Обновить ТС", description = "Обновляет существующее транспортное средство")
    public ResponseEntity<Vehicle> updateVehicle(
            @Parameter(description = "Идентификатор ТС") @PathVariable Long id,
            @Parameter(description = "Обновленные данные ТС")
            @Valid @RequestBody Vehicle vehicle) {

        if (!vehicleService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        vehicle.setId(id);
        return ResponseEntity.ok(vehicleService.save(vehicle));
    }

    /**
     * Удаление транспортного средства.
     *
     * @param id идентификатор ТС
     * @return результат операции
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить ТС", description = "Удаляет транспортное средство по указанному идентификатору")
    public ResponseEntity<Void> deleteVehicle(
            @Parameter(description = "Идентификатор ТС") @PathVariable Long id) {

        if (!vehicleService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        vehicleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Обновление уровня топлива в транспортном средстве.
     *
     * @param id идентификатор ТС
     * @param fuelLevel новый уровень топлива в литрах
     * @return обновленное ТС
     */
    @PutMapping("/{id}/fuel-level")
    @Operation(summary = "Обновить уровень топлива",
            description = "Устанавливает текущий уровень топлива в транспортном средстве")
    public ResponseEntity<Vehicle> updateFuelLevel(
            @Parameter(description = "Идентификатор ТС") @PathVariable Long id,
            @Parameter(description = "Уровень топлива в литрах")
            @RequestParam BigDecimal fuelLevel) {

        Vehicle vehicle = vehicleService.findById(id).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }

        vehicle.setCurrentFuelLevelLitres(fuelLevel);
        return ResponseEntity.ok(vehicleService.save(vehicle));
    }

    /**
     * Обновление показаний одометра транспортного средства.
     *
     * @param id идентификатор ТС
     * @param odometerValue новое значение одометра в километрах
     * @return обновленное ТС
     */
    @PutMapping("/{id}/odometer")
    @Operation(summary = "Обновить показания одометра",
            description = "Устанавливает текущие показания одометра транспортного средства")
    public ResponseEntity<Vehicle> updateOdometer(
            @Parameter(description = "Идентификатор ТС") @PathVariable Long id,
            @Parameter(description = "Показания одометра в километрах")
            @RequestParam BigDecimal odometerValue) {

        Vehicle vehicle = vehicleService.findById(id).orElse(null);
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }

        vehicle.setCurrentOdometerKm(odometerValue);
        return ResponseEntity.ok(vehicleService.save(vehicle));
    }
}
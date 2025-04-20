package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.driver.DriverDetailDto;
import ru.maslov.trucknavigator.dto.driver.DriverRestAnalysisDto;
import ru.maslov.trucknavigator.dto.driver.DriverSummaryDto;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.service.DriverRestTimeService;
import ru.maslov.trucknavigator.service.DriverService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Контроллер для работы с водителями.
 * Предоставляет API для создания, получения и обновления данных водителей,
 * а также для анализа режима труда и отдыха.
 */
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Водители", description = "API для работы с данными водителей и анализа РТО")
public class DriverController {

    private final DriverService driverService;
    private final DriverRestTimeService driverRestTimeService;

    /**
     * Получение списка всех водителей.
     *
     * @return список водителей в формате DTO
     */
    @GetMapping
    @Operation(summary = "Получить всех водителей", description = "Возвращает список всех водителей")
    public ResponseEntity<List<DriverSummaryDto>> getAllDrivers() {
        return ResponseEntity.ok(driverService.findAllSummaries());
    }

    /**
     * Получение водителя по ID.
     *
     * @param id идентификатор водителя
     * @return водитель в формате детального DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Получить водителя по ID", description = "Возвращает водителя по указанному идентификатору")
    public ResponseEntity<DriverDetailDto> getDriverById(
            @Parameter(description = "Идентификатор водителя") @PathVariable Long id) {
        return ResponseEntity.ok(driverService.findDetailById(id));
    }

    /**
     * Создание нового водителя.
     *
     * @param driver данные нового водителя
     * @return созданный водитель в формате детального DTO
     */
    @PostMapping
    @Operation(summary = "Создать водителя", description = "Создает нового водителя на основе переданных данных")
    public ResponseEntity<DriverDetailDto> createDriver(
            @Parameter(description = "Данные водителя")
            @Valid @RequestBody Driver driver) {
        return ResponseEntity.ok(driverService.saveAndGetDto(driver));
    }

    /**
     * Обновление водителя.
     *
     * @param id идентификатор водителя
     * @param driver обновленные данные водителя
     * @return обновленный водитель в формате детального DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Обновить водителя", description = "Обновляет существующего водителя")
    public ResponseEntity<DriverDetailDto> updateDriver(
            @Parameter(description = "Идентификатор водителя") @PathVariable Long id,
            @Parameter(description = "Обновленные данные водителя")
            @Valid @RequestBody Driver driver) {

        if (!driverService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        driver.setId(id);
        return ResponseEntity.ok(driverService.saveAndGetDto(driver));
    }

    /**
     * Удаление водителя.
     *
     * @param id идентификатор водителя
     * @return результат операции
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить водителя", description = "Удаляет водителя по указанному идентификатору")
    public ResponseEntity<Void> deleteDriver(
            @Parameter(description = "Идентификатор водителя") @PathVariable Long id) {

        driverService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Анализ режима труда и отдыха водителя для маршрута.
     *
     * @param driverId идентификатор водителя
     * @param route данные маршрута
     * @param departureTime время отправления
     * @return результат анализа РТО
     */
    @PostMapping("/{driverId}/analyze-rest-time")
    @Operation(summary = "Анализ РТО",
            description = "Анализирует режим труда и отдыха водителя для заданного маршрута")
    public ResponseEntity<DriverRestAnalysisDto> analyzeDriverRestTime(
            @Parameter(description = "Идентификатор водителя") @PathVariable Long driverId,
            @Parameter(description = "Данные маршрута") @RequestBody RouteResponseDto route,
            @Parameter(description = "Время отправления (в формате ISO)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureTime) {

        Driver driver = driverService.findById(driverId).orElse(null);
        if (driver == null || route == null) {
            return ResponseEntity.badRequest().build();
        }

        DriverRestAnalysisDto analysis = driverRestTimeService.analyzeDriverRestTime(driver, route, departureTime);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Обновление статуса водителя (режим труда и отдыха).
     *
     * @param driverId идентификатор водителя
     * @param status новый статус водителя
     * @param timestamp время изменения статуса
     * @return обновленный водитель в формате детального DTO
     */
    @PutMapping("/{driverId}/status")
    @Operation(summary = "Обновить статус водителя",
            description = "Обновляет статус водителя в системе контроля РТО")
    public ResponseEntity<DriverDetailDto> updateDriverStatus(
            @Parameter(description = "Идентификатор водителя") @PathVariable Long driverId,
            @Parameter(description = "Новый статус (DRIVING, REST_BREAK, DAILY_REST и т.д.)")
            @RequestParam Driver.DrivingStatus status,
            @Parameter(description = "Время изменения статуса (в формате ISO)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp) {

        Driver driver = driverService.findById(driverId).orElse(null);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }

        driver = driverRestTimeService.updateDriverStatus(driver, status, timestamp);
        driver = driverService.save(driver);

        return ResponseEntity.ok(driverService.findDetailById(driver.getId()));
    }
}
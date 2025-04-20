package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.cargo.CargoDetailDto;
import ru.maslov.trucknavigator.dto.cargo.CargoSummaryDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.service.CargoService;

import java.util.List;

/**
 * Контроллер для работы с грузами.
 * Предоставляет API для создания, получения и обновления данных грузов.
 */
@RestController
@RequestMapping("/api/cargos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Грузы", description = "API для работы с данными грузов")
public class CargoController {

    private final CargoService cargoService;

    /**
     * Получение списка всех грузов.
     *
     * @return список грузов в формате DTO
     */
    @GetMapping
    @Operation(summary = "Получить все грузы", description = "Возвращает список всех грузов")
    public ResponseEntity<List<CargoSummaryDto>> getAllCargos() {
        return ResponseEntity.ok(cargoService.findAllSummaries());
    }

    /**
     * Получение груза по ID.
     *
     * @param id идентификатор груза
     * @return груз в формате детального DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Получить груз по ID", description = "Возвращает груз по указанному идентификатору")
    public ResponseEntity<CargoDetailDto> getCargoById(
            @Parameter(description = "Идентификатор груза") @PathVariable Long id) {
        return ResponseEntity.ok(cargoService.findDetailById(id));
    }

    /**
     * Создание нового груза.
     *
     * @param cargo данные нового груза
     * @return созданный груз в формате детального DTO
     */
    @PostMapping
    @Operation(summary = "Создать груз", description = "Создает новый груз на основе переданных данных")
    public ResponseEntity<CargoDetailDto> createCargo(
            @Parameter(description = "Данные груза")
            @Valid @RequestBody Cargo cargo) {
        return ResponseEntity.ok(cargoService.saveAndGetDto(cargo));
    }

    /**
     * Обновление груза.
     *
     * @param id идентификатор груза
     * @param cargo обновленные данные груза
     * @return обновленный груз в формате детального DTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Обновить груз", description = "Обновляет существующий груз")
    public ResponseEntity<CargoDetailDto> updateCargo(
            @Parameter(description = "Идентификатор груза") @PathVariable Long id,
            @Parameter(description = "Обновленные данные груза")
            @Valid @RequestBody Cargo cargo) {

        if (!cargoService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        cargo.setId(id);
        return ResponseEntity.ok(cargoService.saveAndGetDto(cargo));
    }

    /**
     * Удаление груза.
     *
     * @param id идентификатор груза
     * @return результат операции
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить груз", description = "Удаляет груз по указанному идентификатору")
    public ResponseEntity<Void> deleteCargo(
            @Parameter(description = "Идентификатор груза") @PathVariable Long id) {

        cargoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
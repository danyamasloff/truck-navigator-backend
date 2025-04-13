package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.routing.RouteRequestDto;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.integration.graphhopper.GraphHopperService;
import ru.maslov.trucknavigator.service.CargoService;
import ru.maslov.trucknavigator.service.DriverService;
import ru.maslov.trucknavigator.service.ProfitabilityService;
import ru.maslov.trucknavigator.service.RiskAnalysisService;
import ru.maslov.trucknavigator.service.RouteService;
import ru.maslov.trucknavigator.service.VehicleService;

import java.util.List;

/**
 * Контроллер для работы с маршрутами.
 * Предоставляет API для создания, получения и обновления маршрутов,
 * а также для выполнения расчетов и анализа маршрутов.
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Маршруты", description = "API для работы с маршрутами грузоперевозок")
public class RouteController {

    private final RouteService routeService;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final CargoService cargoService;
    private final GraphHopperService graphHopperService;
    private final RiskAnalysisService riskAnalysisService;
    private final ProfitabilityService profitabilityService;

    /**
     * Получение списка всех маршрутов.
     *
     * @return список маршрутов
     */
    @GetMapping
    @Operation(summary = "Получить все маршруты", description = "Возвращает список всех маршрутов")
    public ResponseEntity<List<Route>> getAllRoutes() {
        return ResponseEntity.ok(routeService.findAll());
    }

    /**
     * Получение маршрута по ID.
     *
     * @param id идентификатор маршрута
     * @return маршрут
     */
    @GetMapping("/{id}")
    @Operation(summary = "Получить маршрут по ID", description = "Возвращает маршрут по указанному идентификатору")
    public ResponseEntity<Route> getRouteById(
            @Parameter(description = "Идентификатор маршрута") @PathVariable Long id) {
        return routeService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Расчет маршрута на основе запроса.
     *
     * @param request запрос на расчет маршрута
     * @return рассчитанный маршрут с дополнительной информацией
     */
    @PostMapping("/calculate")
    @Operation(summary = "Рассчитать маршрут",
            description = "Рассчитывает маршрут на основе запроса с анализом рисков и экономических показателей")
    public ResponseEntity<RouteResponseDto> calculateRoute(
            @Parameter(description = "Параметры запроса для расчета маршрута")
            @Valid @RequestBody RouteRequestDto request) {

        log.info("Получен запрос на расчет маршрута: {}", request);

        // Получаем связанные сущности
        Vehicle vehicle = request.getVehicleId() != null
                ? vehicleService.findById(request.getVehicleId()).orElse(null)
                : null;

        Driver driver = request.getDriverId() != null
                ? driverService.findById(request.getDriverId()).orElse(null)
                : null;

        Cargo cargo = request.getCargoId() != null
                ? cargoService.findById(request.getCargoId()).orElse(null)
                : null;

        if (vehicle == null) {
            return ResponseEntity.badRequest().build();
        }

        // Выполняем расчет маршрута через GraphHopper
        RouteResponseDto calculatedRoute = graphHopperService.calculateRoute(request, vehicle, cargo);

        // Анализируем риски маршрута
        calculatedRoute = riskAnalysisService.analyzeRouteRisks(calculatedRoute, vehicle, cargo);

        // Рассчитываем экономические показатели
        calculatedRoute = profitabilityService.calculateEconomics(calculatedRoute, vehicle, driver);

        return ResponseEntity.ok(calculatedRoute);
    }

    /**
     * Создание нового маршрута.
     *
     * @param route данные нового маршрута
     * @return созданный маршрут
     */
    @PostMapping
    @Operation(summary = "Создать маршрут", description = "Создает новый маршрут на основе переданных данных")
    public ResponseEntity<Route> createRoute(
            @Parameter(description = "Данные маршрута")
            @Valid @RequestBody Route route) {
        return ResponseEntity.ok(routeService.save(route));
    }

    /**
     * Обновление маршрута.
     *
     * @param id идентификатор маршрута
     * @param route обновленные данные маршрута
     * @return обновленный маршрут
     */
    @PutMapping("/{id}")
    @Operation(summary = "Обновить маршрут", description = "Обновляет существующий маршрут")
    public ResponseEntity<Route> updateRoute(
            @Parameter(description = "Идентификатор маршрута") @PathVariable Long id,
            @Parameter(description = "Обновленные данные маршрута")
            @Valid @RequestBody Route route) {

        if (!routeService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        route.setId(id);
        return ResponseEntity.ok(routeService.save(route));
    }

    /**
     * Удаление маршрута.
     *
     * @param id идентификатор маршрута
     * @return результат операции
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить маршрут", description = "Удаляет маршрут по указанному идентификатору")
    public ResponseEntity<Void> deleteRoute(
            @Parameter(description = "Идентификатор маршрута") @PathVariable Long id) {

        if (!routeService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        routeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
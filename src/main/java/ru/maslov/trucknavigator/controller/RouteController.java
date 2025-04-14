package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.compliance.ComplianceResultDto;
import ru.maslov.trucknavigator.dto.geocoding.GeoPoint;
import ru.maslov.trucknavigator.dto.routing.RouteRequestDto;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.exception.GeocodingException;
import ru.maslov.trucknavigator.exception.RoutingException;
import ru.maslov.trucknavigator.integration.graphhopper.GeocodingService;
import ru.maslov.trucknavigator.integration.graphhopper.GraphHopperService;
import ru.maslov.trucknavigator.service.*;

import java.math.BigDecimal;
import java.util.ArrayList;
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
@SecurityRequirement(name = "bearerAuth")
public class RouteController {

    private final RouteService routeService;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final CargoService cargoService;
    private final GraphHopperService graphHopperService;
    private final GeocodingService geocodingService;
    private final RiskAnalysisService riskAnalysisService;
    private final ProfitabilityService profitabilityService;
    private final ComplianceService complianceService;

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

        // Проверяем соответствие РТО, если указан водитель
        if (driver != null) {
            ComplianceResultDto complianceResult = complianceService.checkRtoCompliance(driver, calculatedRoute.getDuration());
            calculatedRoute.setRtoCompliant(complianceResult.isCompliant());
            calculatedRoute.setRtoWarnings(complianceResult.getWarnings());
        }

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

    @GetMapping("/plan")
    @Operation(summary = "Спланировать маршрут по координатам",
            description = "Рассчитывает маршрут между двумя точками с учетом параметров ТС")
    public ResponseEntity<RouteResponseDto> planRoute(
            @Parameter(description = "Широта начальной точки") @RequestParam double fromLat,
            @Parameter(description = "Долгота начальной точки") @RequestParam double fromLon,
            @Parameter(description = "Широта конечной точки") @RequestParam double toLat,
            @Parameter(description = "Долгота конечной точки") @RequestParam double toLon,
            @Parameter(description = "Тип ТС (car, truck)") @RequestParam(required = false, defaultValue = "car") String vehicleType) {

        log.info("Получен запрос на планирование маршрута: {} -> {}, тип ТС: {}",
                fromLat + "," + fromLon,
                toLat + "," + toLon,
                vehicleType);

        try {
            // Создаем запрос для построения маршрута
            RouteRequestDto request = RouteRequestDto.builder()
                    .startLat(fromLat)
                    .startLon(fromLon)
                    .endLat(toLat)
                    .endLon(toLon)
                    .startAddress("Начальная точка")  // Добавляем для логов
                    .endAddress("Конечная точка")     // Добавляем для логов
                    .build();

            // Создаем временный объект Vehicle с параметрами, соответствующими типу ТС
            Vehicle vehicleEntity = createVehicleByType(vehicleType);

            // Добавляем информацию о типе ТС в запрос
            request.setName(vehicleType);

            // Расчет маршрута и применение аналитики
            return calculateAndAnalyzeRoute(request, vehicleEntity, null);

        } catch (RoutingException e) {
            log.error("Ошибка маршрутизации: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponseDto(e.getMessage()));
        } catch (Exception e) {
            log.error("Необработанная ошибка: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponseDto("Внутренняя ошибка сервера"));
        }
    }

    @GetMapping("/plan-by-name")
    @Operation(summary = "Спланировать маршрут по названиям мест",
            description = "Рассчитывает маршрут между двумя населенными пунктами с учетом параметров ТС")
    public ResponseEntity<RouteResponseDto> planRouteByNames(
            @Parameter(description = "Название начальной точки (город, адрес)") @RequestParam String fromPlace,
            @Parameter(description = "Название конечной точки (город, адрес)") @RequestParam String toPlace,
            @Parameter(description = "Тип ТС (car, truck)") @RequestParam(required = false, defaultValue = "car") String vehicleType) {

        log.info("Получен запрос на планирование маршрута по названиям: {} -> {}, тип ТС: {}",
                fromPlace, toPlace, vehicleType);

        try {
            // Геокодирование названий мест в координаты
            GeoPoint fromPoint = geocodingService.geocode(fromPlace);
            GeoPoint toPoint = geocodingService.geocode(toPlace);

            log.info("Геокодирование успешно. {} -> [{}, {}], {} -> [{}, {}]",
                    fromPlace, fromPoint.getLat(), fromPoint.getLng(),
                    toPlace, toPoint.getLat(), toPoint.getLng());

            // Создаем запрос для построения маршрута
            RouteRequestDto request = RouteRequestDto.builder()
                    .startLat(fromPoint.getLat())
                    .startLon(fromPoint.getLng())
                    .endLat(toPoint.getLat())
                    .endLon(toPoint.getLng())
                    .startAddress(fromPlace)
                    .endAddress(toPlace)
                    .build();

            // Создаем временный объект Vehicle с параметрами типа ТС
            Vehicle vehicleEntity = createVehicleByType(vehicleType);

            // Добавляем информацию о типе ТС в запрос
            request.setName(vehicleType);

            // Расчет маршрута и применение аналитики
            return calculateAndAnalyzeRoute(request, vehicleEntity, null);

        } catch (GeocodingException e) {
            log.error("Ошибка геокодирования: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponseDto(e.getMessage()));
        } catch (RoutingException e) {
            log.error("Ошибка маршрутизации: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponseDto(e.getMessage()));
        } catch (Exception e) {
            log.error("Необработанная ошибка: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponseDto("Внутренняя ошибка сервера"));
        }
    }

    /**
     * Создает объект Vehicle с параметрами для указанного типа транспорта.
     *
     * @param vehicleType тип транспортного средства (car, truck)
     * @return объект Vehicle с соответствующими параметрами
     */
    private Vehicle createVehicleByType(String vehicleType) {
        if ("car".equalsIgnoreCase(vehicleType)) {
            // Параметры для легкового автомобиля
            return Vehicle.builder()
                    .heightCm(180)       // 1.8 метра
                    .widthCm(200)        // 2.0 метра
                    .lengthCm(450)       // 4.5 метра
                    .emptyWeightKg(1500) // 1.5 тонны
                    .grossWeightKg(2000) // 2 тонны полного веса
                    .fuelConsumptionPer100km(new BigDecimal("8.0"))
                    .build();
        } else {
            // Параметры для грузового автомобиля
            return Vehicle.builder()
                    .heightCm(400)        // 4.0 метра
                    .widthCm(250)         // 2.5 метра
                    .lengthCm(1200)       // 12 метров
                    .emptyWeightKg(10000) // 10 тонн
                    .grossWeightKg(18000) // 18 тонн полного веса
                    .fuelConsumptionPer100km(new BigDecimal("32.0"))
                    .build();
        }
    }

    /**
     * Выполняет расчет маршрута и применяет к нему аналитику рисков и экономики.
     *
     * @param request запрос на расчет маршрута
     * @param vehicle транспортное средство
     * @param cargo груз (может быть null)
     * @return ResponseEntity с результатом или ошибкой
     */
    private ResponseEntity<RouteResponseDto> calculateAndAnalyzeRoute(
            RouteRequestDto request, Vehicle vehicle, Cargo cargo) {

        // Расчет маршрута с использованием GraphHopper
        RouteResponseDto calculatedRoute = graphHopperService.calculateRoute(request, vehicle, cargo);

        // Если маршрут не удалось рассчитать, возвращаем ошибку
        if (calculatedRoute == null || calculatedRoute.getCoordinates() == null
                || calculatedRoute.getCoordinates().isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(RouteResponseDto.builder()
                            .distance(BigDecimal.ZERO)
                            .duration(0)
                            .build());
        }

        // Добавляем анализ рисков и экономические показатели
        try {
            calculatedRoute = riskAnalysisService.analyzeRouteRisks(calculatedRoute, vehicle, cargo);
        } catch (Exception e) {
            log.warn("Ошибка при анализе рисков: {}", e.getMessage());
            // Продолжаем выполнение без анализа рисков
        }

        try {
            calculatedRoute = profitabilityService.calculateEconomics(calculatedRoute, vehicle, null);
        } catch (Exception e) {
            log.warn("Ошибка при расчете экономики: {}", e.getMessage());
            // Продолжаем выполнение без экономических показателей
        }

        return ResponseEntity.ok(calculatedRoute);
    }

    /**
     * Создает DTO с информацией об ошибке.
     */
    private RouteResponseDto createErrorResponseDto(String errorMessage) {
        RouteResponseDto errorDto = new RouteResponseDto();
        errorDto.setDistance(BigDecimal.ZERO);
        errorDto.setDuration(0);
        errorDto.setCoordinates(new ArrayList<>());
        errorDto.setInstructions(new ArrayList<>());

        // Добавляем инструкцию с сообщением об ошибке
        RouteResponseDto.Instruction errorInstruction = new RouteResponseDto.Instruction();
        errorInstruction.setText("Ошибка: " + errorMessage);
        errorInstruction.setDistance(BigDecimal.ZERO);
        errorInstruction.setTime(0);
        errorDto.getInstructions().add(errorInstruction);

        return errorDto;
    }
}
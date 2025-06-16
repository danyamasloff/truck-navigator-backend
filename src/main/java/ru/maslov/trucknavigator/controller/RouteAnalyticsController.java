package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.analytics.RouteAnalyticsDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.service.CargoService;
import ru.maslov.trucknavigator.service.DriverService;
import ru.maslov.trucknavigator.service.RouteService;
import ru.maslov.trucknavigator.service.VehicleService;
import ru.maslov.trucknavigator.service.analytics.RouteAnalyticsService;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Аналитика", description = "API для комплексного анализа маршрутов")
public class RouteAnalyticsController {

    private final RouteAnalyticsService analyticsService;
    private final RouteService routeService;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final CargoService cargoService;

    @GetMapping("/route/{routeId}")
    @Operation(summary = "Комплексный анализ маршрута",
            description = "Выполняет полный анализ маршрута с учетом ТС, водителя и груза")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<RouteAnalyticsDto> analyzeRoute(
            @PathVariable Long routeId,
            @RequestParam(required = false, defaultValue = "true") boolean includeWeather) {

        Route route = routeService.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Маршрут не найден"));

        Vehicle vehicle = route.getVehicle();
        Driver driver = route.getDriver();
        Cargo cargo = route.getCargo();

        RouteAnalyticsDto analytics = analyticsService.analyzeRoute(
                route, vehicle, driver, cargo, includeWeather);

        return ResponseEntity.ok(analytics);
    }

    @PostMapping("/route/{routeId}/with-params")
    @Operation(summary = "Анализ маршрута с заданными параметрами",
            description = "Позволяет выполнить анализ с произвольными ТС, водителем и грузом")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<RouteAnalyticsDto> analyzeRouteWithParams(
            @PathVariable Long routeId,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Long cargoId,
            @RequestParam(required = false, defaultValue = "true") boolean includeWeather) {

        Route route = routeService.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Маршрут не найден"));

        Vehicle vehicle = vehicleId != null ?
                vehicleService.findById(vehicleId).orElse(route.getVehicle()) :
                route.getVehicle();

        Driver driver = driverId != null ?
                driverService.findById(driverId).orElse(route.getDriver()) :
                route.getDriver();

        Cargo cargo = cargoId != null ?
                cargoService.findById(cargoId).orElse(route.getCargo()) :
                route.getCargo();

        RouteAnalyticsDto analytics = analyticsService.analyzeRoute(
                route, vehicle, driver, cargo, includeWeather);

        return ResponseEntity.ok(analytics);
    }
}

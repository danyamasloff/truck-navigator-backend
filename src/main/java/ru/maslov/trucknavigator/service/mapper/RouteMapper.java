package ru.maslov.trucknavigator.service.mapper;

import org.springframework.stereotype.Component;
import ru.maslov.trucknavigator.dto.routing.RouteCreateUpdateDto;
import ru.maslov.trucknavigator.dto.routing.RouteDetailDto;
import ru.maslov.trucknavigator.dto.routing.RouteSummaryDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.entity.Waypoint;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Сервис для преобразования между сущностями и DTO маршрутов.
 */
@Component
public class RouteMapper {

    /**
     * Преобразует маршрут в DTO для списка.
     */
    public RouteSummaryDto toSummaryDto(Route route) {
        RouteSummaryDto dto = new RouteSummaryDto();
        dto.setId(route.getId());
        dto.setName(route.getName());
        dto.setStartAddress(route.getStartAddress());
        dto.setEndAddress(route.getEndAddress());
        dto.setDepartureTime(route.getDepartureTime());
        dto.setEstimatedArrivalTime(route.getEstimatedArrivalTime());
        dto.setDistanceKm(route.getDistanceKm());
        dto.setEstimatedDurationMinutes(route.getEstimatedDurationMinutes());
        dto.setEstimatedTotalCost(route.getEstimatedTotalCost());
        dto.setStatus(route.getStatus());
        dto.setOverallRiskScore(route.getOverallRiskScore());

        // Дополнительные поля из связанных сущностей
        if (route.getVehicle() != null) {
            dto.setVehicleName(route.getVehicle().getModel() + " (" +
                    route.getVehicle().getRegistrationNumber() + ")");
        }

        if (route.getDriver() != null) {
            dto.setDriverName(route.getDriver().getLastName() + " " +
                    route.getDriver().getFirstName());
        }

        if (route.getCargo() != null) {
            dto.setCargoName(route.getCargo().getName());
        }

        return dto;
    }

    /**
     * Преобразует маршрут в детальное DTO.
     */
    public RouteDetailDto toDetailDto(Route route) {
        RouteDetailDto dto = new RouteDetailDto();
        dto.setId(route.getId());
        dto.setName(route.getName());

        // Маппинг связанных сущностей
        if (route.getVehicle() != null) {
            Vehicle v = route.getVehicle();
            dto.setVehicle(new RouteDetailDto.VehicleDto(
                    v.getId(), v.getRegistrationNumber(), v.getModel()));
        }

        if (route.getDriver() != null) {
            Driver d = route.getDriver();
            dto.setDriver(new RouteDetailDto.DriverDto(
                    d.getId(), d.getFirstName(), d.getLastName(), d.getLicenseNumber()));
        }

        if (route.getCargo() != null) {
            Cargo c = route.getCargo();
            dto.setCargo(new RouteDetailDto.CargoDto(
                    c.getId(), c.getName(), c.getCargoType().toString(), c.getWeightKg()));
        }

        // Маппинг основных полей
        dto.setStartAddress(route.getStartAddress());
        dto.setStartLat(route.getStartLat());
        dto.setStartLon(route.getStartLon());
        dto.setEndAddress(route.getEndAddress());
        dto.setEndLat(route.getEndLat());
        dto.setEndLon(route.getEndLon());

        dto.setDepartureTime(route.getDepartureTime());
        dto.setEstimatedArrivalTime(route.getEstimatedArrivalTime());
        dto.setActualArrivalTime(route.getActualArrivalTime());

        // Координаты получаем из LineString, если доступны
        if (route.getRouteGeometry() != null) {
            // Для простоты примера просто используем пустой список
            // В реальности здесь будет конвертация из LineString в массивы координат
            dto.setCoordinates(new ArrayList<>());
        }

        // Маппинг промежуточных точек
        if (route.getWaypoints() != null) {
            dto.setWaypoints(route.getWaypoints().stream()
                    .map(w -> mapWaypoint(w))
                    .collect(Collectors.toList()));
        }

        // Параметры маршрута
        dto.setDistanceKm(route.getDistanceKm());
        dto.setEstimatedDurationMinutes(route.getEstimatedDurationMinutes());

        // Экономика
        dto.setEstimatedFuelConsumption(route.getEstimatedFuelConsumption());
        dto.setActualFuelConsumption(route.getActualFuelConsumption());
        dto.setEstimatedFuelCost(route.getEstimatedFuelCost());
        dto.setEstimatedTollCost(route.getEstimatedTollCost());
        dto.setEstimatedDriverCost(route.getEstimatedDriverCost());
        dto.setEstimatedTotalCost(route.getEstimatedTotalCost());
        dto.setActualTotalCost(route.getActualTotalCost());
        dto.setCurrency(route.getCurrency());

        // Анализ рисков
        dto.setOverallRiskScore(route.getOverallRiskScore());
        dto.setWeatherRiskScore(route.getWeatherRiskScore());
        dto.setRoadQualityRiskScore(route.getRoadQualityRiskScore());
        dto.setTrafficRiskScore(route.getTrafficRiskScore());
        dto.setCargoRiskScore(route.getCargoRiskScore());

        // Статус и метаданные
        dto.setStatus(route.getStatus());
        dto.setCreatedAt(route.getCreatedAt());
        dto.setUpdatedAt(route.getUpdatedAt());

        return dto;
    }

    private RouteDetailDto.WaypointDto mapWaypoint(Waypoint waypoint) {
        return new RouteDetailDto.WaypointDto(
                waypoint.getId(),
                waypoint.getName(),
                waypoint.getAddress(),
                waypoint.getLatitude(),
                waypoint.getLongitude(),
                waypoint.getWaypointType().toString(),
                waypoint.getPlannedArrivalTime(),
                waypoint.getPlannedDepartureTime(),
                waypoint.getActualArrivalTime(),
                waypoint.getActualDepartureTime(),
                waypoint.getStayDurationMinutes()
        );
    }

    /**
     * Преобразует DTO создания/обновления в сущность.
     * Для обновления требуется существующая сущность.
     */
    public Route toEntity(RouteCreateUpdateDto dto, Route existingRoute,
                          Vehicle vehicle, Driver driver, Cargo cargo) {
        Route route = existingRoute != null ? existingRoute : new Route();

        route.setName(dto.getName());
        route.setVehicle(vehicle);
        route.setDriver(driver);
        route.setCargo(cargo);

        route.setStartAddress(dto.getStartAddress());
        route.setStartLat(dto.getStartLat());
        route.setStartLon(dto.getStartLon());
        route.setEndAddress(dto.getEndAddress());
        route.setEndLat(dto.getEndLat());
        route.setEndLon(dto.getEndLon());

        route.setDepartureTime(dto.getDepartureTime());

        if (dto.getStatus() != null) {
            route.setStatus(dto.getStatus());
        } else if (existingRoute == null) {
            // Устанавливаем статус DRAFT по умолчанию при создании
            route.setStatus(Route.RouteStatus.DRAFT);
        }

        // Примечание: промежуточные точки обычно обрабатываются отдельно
        // через специальный сервис для создания/обновления Waypoint

        return route;
    }
}
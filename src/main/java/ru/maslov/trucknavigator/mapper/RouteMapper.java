package ru.maslov.trucknavigator.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.maslov.trucknavigator.dto.routing.RouteCreateUpdateDto;
import ru.maslov.trucknavigator.dto.routing.RouteDetailDto;
import ru.maslov.trucknavigator.dto.routing.RouteSummaryDto;
import ru.maslov.trucknavigator.dto.routing.WaypointDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.entity.Waypoint;
import ru.maslov.trucknavigator.service.WaypointService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования между сущностями и DTO маршрутов.
 */
@Component
@RequiredArgsConstructor
public class RouteMapper {

    private final WaypointService waypointService;

    /**
     * Преобразует маршрут в DTO для списка.
     */
    public RouteSummaryDto toSummaryDto(Route route) {
        if (route == null) {
            return null;
        }

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
            Vehicle v = route.getVehicle();
            dto.setVehicleName(v.getModel() + " (" + v.getRegistrationNumber() + ")");
        }

        if (route.getDriver() != null) {
            Driver d = route.getDriver();
            dto.setDriverName(d.getLastName() + " " + d.getFirstName());
        }

        if (route.getCargo() != null) {
            Cargo c = route.getCargo();
            dto.setCargoName(c.getName());
        }

        return dto;
    }

    /**
     * Преобразует маршрут в детальное DTO.
     */
    public RouteDetailDto toDetailDto(Route route) {
        if (route == null) {
            return null;
        }

        RouteDetailDto dto = new RouteDetailDto();
        dto.setId(route.getId());
        dto.setName(route.getName());

        // Маппинг связанных сущностей
        mapRelatedEntities(route, dto);

        // Маппинг основных полей
        mapBasicFields(route, dto);

        // Получение промежуточных точек из сервиса, если они не загружены с сущностью
        List<Waypoint> waypoints = route.getWaypoints();
        if (waypoints == null || waypoints.isEmpty()) {
            waypoints = waypointService.findByRouteId(route.getId());
        }

        // Маппинг промежуточных точек
        if (waypoints != null && !waypoints.isEmpty()) {
            dto.setWaypoints(waypoints.stream()
                    .map(this::mapWaypointToRouteDetailWaypointDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setWaypoints(Collections.emptyList());
        }

        // Получаем координаты из LineString, если доступны
        extractCoordinatesFromGeometry(route, dto);

        // Маппинг параметров маршрута и экономики
        mapRouteParameters(route, dto);

        // Анализ рисков
        mapRiskAnalysis(route, dto);

        // Статус и метаданные
        dto.setStatus(route.getStatus());
        dto.setCreatedAt(route.getCreatedAt());
        dto.setUpdatedAt(route.getUpdatedAt());

        return dto;
    }

    /**
     * Преобразует DTO создания/обновления в сущность.
     */
    public Route toEntity(RouteCreateUpdateDto dto, Route existingRoute,
                          Vehicle vehicle, Driver driver, Cargo cargo) {
        if (dto == null) {
            return existingRoute;
        }

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

        // Добавляем значения по умолчанию для обязательных полей
        if (existingRoute == null || route.getDistanceKm() == null) {
            route.setDistanceKm(BigDecimal.ZERO); // Значение по умолчанию для нового маршрута
        }

        if (existingRoute == null || route.getEstimatedDurationMinutes() == null) {
            route.setEstimatedDurationMinutes(0); // Значение по умолчанию
        }

        if (dto.getStatus() != null) {
            route.setStatus(dto.getStatus());
        } else if (existingRoute == null) {
            // Устанавливаем статус DRAFT по умолчанию при создании
            route.setStatus(Route.RouteStatus.DRAFT);
        }

        return route;
    }

    /**
     * Преобразует WaypointDto в WaypointDto для RouteDetailDto.
     */
    public RouteDetailDto.WaypointDto waypointDtoToRouteDetailWaypointDto(WaypointDto waypointDto) {
        if (waypointDto == null) {
            return null;
        }

        return new RouteDetailDto.WaypointDto(
                waypointDto.getId(),
                waypointDto.getName(),
                waypointDto.getAddress(),
                waypointDto.getLatitude(),
                waypointDto.getLongitude(),
                waypointDto.getType(),
                waypointDto.getPlannedArrivalTime(),
                waypointDto.getPlannedDepartureTime(),
                waypointDto.getActualArrivalTime(),
                waypointDto.getActualDepartureTime(),
                waypointDto.getStayDurationMinutes()
        );
    }

    /**
     * Преобразует сущность Waypoint в WaypointDto.
     */
    public WaypointDto mapWaypointToDto(Waypoint waypoint) {
        if (waypoint == null) {
            return null;
        }

        return WaypointDto.builder()
                .id(waypoint.getId())
                .name(waypoint.getName())
                .address(waypoint.getAddress())
                .latitude(waypoint.getLatitude())
                .longitude(waypoint.getLongitude())
                .type(waypoint.getWaypointType() != null ? waypoint.getWaypointType().toString() : null)
                .orderIndex(waypoint.getOrderIndex())
                .plannedArrivalTime(waypoint.getPlannedArrivalTime())
                .plannedDepartureTime(waypoint.getPlannedDepartureTime())
                .actualArrivalTime(waypoint.getActualArrivalTime())
                .actualDepartureTime(waypoint.getActualDepartureTime())
                .stayDurationMinutes(waypoint.getStayDurationMinutes())
                .additionalInfo(waypoint.getAdditionalInfo())
                .routeId(waypoint.getRoute() != null ? waypoint.getRoute().getId() : null)
                .build();
    }

    /**
     * Преобразует WaypointDto в сущность Waypoint.
     */
    public Waypoint mapDtoToWaypoint(WaypointDto dto, Route route) {
        if (dto == null) {
            return null;
        }

        Waypoint waypoint = new Waypoint();
        if (dto.getId() != null) {
            waypoint.setId(dto.getId());
        }

        waypoint.setName(dto.getName());
        waypoint.setAddress(dto.getAddress());
        waypoint.setLatitude(dto.getLatitude());
        waypoint.setLongitude(dto.getLongitude());

        if (dto.getType() != null) {
            try {
                waypoint.setWaypointType(Waypoint.WaypointType.valueOf(dto.getType()));
            } catch (IllegalArgumentException e) {
                waypoint.setWaypointType(Waypoint.WaypointType.WAYPOINT);
            }
        }

        waypoint.setOrderIndex(dto.getOrderIndex());
        waypoint.setPlannedArrivalTime(dto.getPlannedArrivalTime());
        waypoint.setPlannedDepartureTime(dto.getPlannedDepartureTime());
        waypoint.setActualArrivalTime(dto.getActualArrivalTime());
        waypoint.setActualDepartureTime(dto.getActualDepartureTime());
        waypoint.setStayDurationMinutes(dto.getStayDurationMinutes());
        waypoint.setAdditionalInfo(dto.getAdditionalInfo());
        waypoint.setRoute(route);

        return waypoint;
    }

    /**
     * Преобразует WaypointDto из RouteCreateUpdateDto в WaypointDto.
     */
    public WaypointDto mapCreateUpdateWaypointToDto(RouteCreateUpdateDto.WaypointDto dto) {
        if (dto == null) {
            return null;
        }

        return WaypointDto.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .type(dto.getWaypointType())
                .stayDurationMinutes(dto.getStayDurationMinutes())
                .build();
    }

    /**
     * Преобразует Waypoint в WaypointDto для RouteDetailDto.
     */
    private RouteDetailDto.WaypointDto mapWaypointToRouteDetailWaypointDto(Waypoint waypoint) {
        if (waypoint == null) {
            return null;
        }

        return new RouteDetailDto.WaypointDto(
                waypoint.getId(),
                waypoint.getName(),
                waypoint.getAddress(),
                waypoint.getLatitude(),
                waypoint.getLongitude(),
                waypoint.getWaypointType() != null ? waypoint.getWaypointType().toString() : null,
                waypoint.getPlannedArrivalTime(),
                waypoint.getPlannedDepartureTime(),
                waypoint.getActualArrivalTime(),
                waypoint.getActualDepartureTime(),
                waypoint.getStayDurationMinutes()
        );
    }

    /**
     * Отображает связанные сущности из маршрута в DTO.
     */
    private void mapRelatedEntities(Route route, RouteDetailDto dto) {
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
    }

    /**
     * Отображает основные поля маршрута в DTO.
     */
    private void mapBasicFields(Route route, RouteDetailDto dto) {
        dto.setStartAddress(route.getStartAddress());
        dto.setStartLat(route.getStartLat());
        dto.setStartLon(route.getStartLon());
        dto.setEndAddress(route.getEndAddress());
        dto.setEndLat(route.getEndLat());
        dto.setEndLon(route.getEndLon());

        dto.setDepartureTime(route.getDepartureTime());
        dto.setEstimatedArrivalTime(route.getEstimatedArrivalTime());
        dto.setActualArrivalTime(route.getActualArrivalTime());
    }

    /**
     * Извлекает координаты из геометрии маршрута.
     */
    private void extractCoordinatesFromGeometry(Route route, RouteDetailDto dto) {
        if (route.getRouteGeometry() != null) {
            List<double[]> coordinates = new ArrayList<>();

            try {
                // Код для извлечения точек из LineString
                // Фактическая реализация зависит от библиотеки геопространственных данных
                dto.setCoordinates(coordinates);
            } catch (Exception e) {
                dto.setCoordinates(Collections.emptyList());
            }
        } else {
            dto.setCoordinates(Collections.emptyList());
        }
    }

    /**
     * Отображает параметры маршрута и экономические показатели в DTO.
     */
    private void mapRouteParameters(Route route, RouteDetailDto dto) {
        dto.setDistanceKm(route.getDistanceKm());
        dto.setEstimatedDurationMinutes(route.getEstimatedDurationMinutes());
        dto.setEstimatedFuelConsumption(route.getEstimatedFuelConsumption());
        dto.setActualFuelConsumption(route.getActualFuelConsumption());
        dto.setEstimatedFuelCost(route.getEstimatedFuelCost());
        dto.setEstimatedTollCost(route.getEstimatedTollCost());
        dto.setEstimatedDriverCost(route.getEstimatedDriverCost());
        dto.setEstimatedTotalCost(route.getEstimatedTotalCost());
        dto.setActualTotalCost(route.getActualTotalCost());
        dto.setCurrency(route.getCurrency());
    }

    /**
     * Отображает показатели анализа рисков в DTO.
     */
    private void mapRiskAnalysis(Route route, RouteDetailDto dto) {
        dto.setOverallRiskScore(route.getOverallRiskScore());
        dto.setWeatherRiskScore(route.getWeatherRiskScore());
        dto.setRoadQualityRiskScore(route.getRoadQualityRiskScore());
        dto.setTrafficRiskScore(route.getTrafficRiskScore());
        dto.setCargoRiskScore(route.getCargoRiskScore());
    }
}

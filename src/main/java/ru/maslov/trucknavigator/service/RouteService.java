package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.routing.*;
import ru.maslov.trucknavigator.entity.*;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.mapper.RouteMapper;
import ru.maslov.trucknavigator.repository.RouteRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final RouteRepository routeRepository;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final CargoService cargoService;
    private final RouteMapper routeMapper;
    private final WaypointService waypointService;

    public Optional<Route> findById(Long id) {
        return routeRepository.findById(id);
    }

    @Transactional
    public Route save(Route route) {
        return routeRepository.save(route);
    }

    public List<RouteSummaryDto> findAllSummaries() {
        return routeRepository.findAll()
                .stream()
                .map(routeMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    public RouteDetailDto findDetailById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Route", id));

        // 1) Маппим базовые поля
        RouteDetailDto dto = routeMapper.toDetailDto(route);

        // 2) Точки: Entity → вложенный DTO
        List<RouteDetailDto.WaypointDto> wps = waypointService.listWaypoints(id)
                .stream()
                .map(routeMapper::waypointDtoToRouteDetailWaypointDto)
                .collect(Collectors.toList());
        dto.setWaypoints(wps);

        return dto;
    }

    @Transactional
    public RouteDetailDto createRoute(RouteCreateUpdateDto in) {
        final var ent = getRelatedEntities(in);

        // 1) сохраним базу маршрута
        Route route = routeMapper.toEntity(in, null,
                ent.vehicle, ent.driver, ent.cargo);
        route = routeRepository.save(route);
        final Long routeId = route.getId();

        // 2) сконвертим вложенный DTO → глобальный WaypointDto
        if (in.getWaypoints() != null) {
            List<WaypointDto> wps = in.getWaypoints().stream()
                    .map(src -> createWaypointDto(src, routeId))
                    .collect(Collectors.toList());
            waypointService.createWaypointsForRoute(route, wps);
        }

        // 3) отдадим полные детали с точками
        return findDetailById(routeId);
    }

    @Transactional
    public RouteDetailDto updateRoute(Long id, RouteCreateUpdateDto in) {
        Route existing = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Route", id));
        final var ent = getRelatedEntities(in);

        // 1) обновим базу маршрута
        Route updated = routeMapper.toEntity(in, existing,
                ent.vehicle, ent.driver, ent.cargo);
        updated = routeRepository.save(updated);
        final Long routeId = updated.getId();

        // 2) обновим точки
        if (in.getWaypoints() != null) {
            List<WaypointDto> wps = in.getWaypoints().stream()
                    .map(src -> createWaypointDto(src, routeId))
                    .collect(Collectors.toList());
            waypointService.updateWaypointsForRoute(updated, wps);
        }

        // 3) вернём детали
        return findDetailById(routeId);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!routeRepository.existsById(id)) {
            throw new EntityNotFoundException("Route", id);
        }
        waypointService.deleteAllForRoute(id);
        routeRepository.deleteById(id);
    }

    private WaypointDto createWaypointDto(RouteCreateUpdateDto.WaypointDto src, Long routeId) {
        return WaypointDto.builder()
                .name(src.getName())
                .address(src.getAddress())
                .latitude(src.getLatitude())
                .longitude(src.getLongitude())
                .type(src.getWaypointType())
                .stayDurationMinutes(src.getStayDurationMinutes())
                .routeId(routeId)
                .build();
    }

    /** Вспомогательный метод для vehicle/driver/cargo */
    private record RelatedEntities(
            Vehicle vehicle, Driver driver, Cargo cargo
    ){}
    private RelatedEntities getRelatedEntities(RouteCreateUpdateDto dto) {
        Vehicle v = dto.getVehicleId() == null
                ? null
                : vehicleService.findById(dto.getVehicleId())
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", dto.getVehicleId()));
        Driver d = dto.getDriverId() == null
                ? null
                : driverService.findById(dto.getDriverId())
                .orElseThrow(() -> new EntityNotFoundException("Driver", dto.getDriverId()));
        Cargo c = dto.getCargoId() == null
                ? null
                : cargoService.findById(dto.getCargoId())
                .orElseThrow(() -> new EntityNotFoundException("Cargo", dto.getCargoId()));
        return new RelatedEntities(v, d, c);
    }
}

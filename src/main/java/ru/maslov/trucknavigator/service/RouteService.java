package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.routing.RouteCreateUpdateDto;
import ru.maslov.trucknavigator.dto.routing.RouteDetailDto;
import ru.maslov.trucknavigator.dto.routing.RouteSummaryDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.entity.Waypoint;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.repository.RouteRepository;
import ru.maslov.trucknavigator.service.mapper.RouteMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для работы с маршрутами.
 */
@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final CargoService cargoService;
    private final RouteMapper routeMapper;

    /**
     * Получает все маршруты в виде DTO для списка.
     */
    public List<RouteSummaryDto> findAllSummaries() {
        return routeRepository.findAll().stream()
                .map(routeMapper::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Получает маршрут по идентификатору.
     */
    public Optional<Route> findById(Long id) {
        return routeRepository.findById(id);
    }

    /**
     * Получает детальную информацию о маршруте по идентификатору.
     */
    public RouteDetailDto findDetailById(Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Route", id));
        return routeMapper.toDetailDto(route);
    }

    /**
     * Создает новый маршрут на основе DTO.
     */
    @Transactional
    public RouteDetailDto createRoute(RouteCreateUpdateDto dto) {
        // Получаем связанные сущности
        Vehicle vehicle = null;
        if (dto.getVehicleId() != null) {
            vehicle = vehicleService.findById(dto.getVehicleId())
                    .orElseThrow(() -> new EntityNotFoundException("Vehicle", dto.getVehicleId()));
        }

        Driver driver = null;
        if (dto.getDriverId() != null) {
            driver = driverService.findById(dto.getDriverId())
                    .orElseThrow(() -> new EntityNotFoundException("Driver", dto.getDriverId()));
        }

        Cargo cargo = null;
        if (dto.getCargoId() != null) {
            cargo = cargoService.findById(dto.getCargoId())
                    .orElseThrow(() -> new EntityNotFoundException("Cargo", dto.getCargoId()));
        }

        // Создаем маршрут
        Route route = routeMapper.toEntity(dto, null, vehicle, driver, cargo);

        // Сохраняем маршрут
        route = routeRepository.save(route);

        // Обрабатываем промежуточные точки
        if (dto.getWaypoints() != null && !dto.getWaypoints().isEmpty()) {
            createWaypoints(route, dto.getWaypoints());
        }

        // Возвращаем детали созданного маршрута
        return routeMapper.toDetailDto(route);
    }

    /**
     * Обновляет существующий маршрут.
     */
    @Transactional
    public RouteDetailDto updateRoute(Long id, RouteCreateUpdateDto dto) {
        // Находим существующий маршрут
        Route existingRoute = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Route", id));

        // Получаем связанные сущности
        Vehicle vehicle = null;
        if (dto.getVehicleId() != null) {
            vehicle = vehicleService.findById(dto.getVehicleId())
                    .orElseThrow(() -> new EntityNotFoundException("Vehicle", dto.getVehicleId()));
        }

        Driver driver = null;
        if (dto.getDriverId() != null) {
            driver = driverService.findById(dto.getDriverId())
                    .orElseThrow(() -> new EntityNotFoundException("Driver", dto.getDriverId()));
        }

        Cargo cargo = null;
        if (dto.getCargoId() != null) {
            cargo = cargoService.findById(dto.getCargoId())
                    .orElseThrow(() -> new EntityNotFoundException("Cargo", dto.getCargoId()));
        }

        // Обновляем маршрут
        Route updatedRoute = routeMapper.toEntity(dto, existingRoute, vehicle, driver, cargo);

        // Сохраняем маршрут
        updatedRoute = routeRepository.save(updatedRoute);

        // Обрабатываем промежуточные точки
        if (dto.getWaypoints() != null) {
            // Обычно здесь логика удаления старых и создания новых точек
            // Для простоты примера просто создаем новые
            createWaypoints(updatedRoute, dto.getWaypoints());
        }

        // Возвращаем детали обновленного маршрута
        return routeMapper.toDetailDto(updatedRoute);
    }

    /**
     * Удаляет маршрут по идентификатору.
     */
    @Transactional
    public void deleteById(Long id) {
        if (!routeRepository.existsById(id)) {
            throw new EntityNotFoundException("Route", id);
        }
        routeRepository.deleteById(id);
    }

    /**
     * Проверяет наличие маршрута по идентификатору.
     */
    public boolean existsById(Long id) {
        return routeRepository.existsById(id);
    }

    /**
     * Сохраняет маршрут.
     */
    public Route save(Route route) {
        return routeRepository.save(route);
    }

    /**
     * Получает все маршруты.
     *
     * @return список всех маршрутов
     */
    public List<Route> findAll() {
        return routeRepository.findAll();
    }

    /**
     * Создает промежуточные точки для маршрута.
     */
    private void createWaypoints(Route route, List<RouteCreateUpdateDto.WaypointDto> waypointDtos) {
        // Реализация создания промежуточных точек
        // Здесь должна быть логика создания и сохранения Waypoint сущностей
    }
}
package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.routing.*;
import ru.maslov.trucknavigator.entity.*;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.mapper.RouteMapper;
import ru.maslov.trucknavigator.repository.RouteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteService {
    private final RouteRepository routeRepository;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final CargoService cargoService;
    private final RouteMapper routeMapper;
    private final NotificationService notificationService;

    public Optional<Route> findById(Long id) {
        return routeRepository.findById(id);
    }

    public List<Route> findAll() {
        return routeRepository.findAll();
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

        return routeMapper.toDetailDto(route);
    }

    @Transactional
    public RouteDetailDto createRoute(RouteCreateUpdateDto dto) {
        Route route = new Route();
        
        // Основные поля
        route.setName(dto.getName());
        route.setStartAddress(dto.getStartAddress());
        route.setEndAddress(dto.getEndAddress());
        
        // Координаты (исправленные названия методов)
        if (dto.getStartLat() != null) route.setStartLat(dto.getStartLat());
        if (dto.getStartLon() != null) route.setStartLon(dto.getStartLon());
        if (dto.getEndLat() != null) route.setEndLat(dto.getEndLat());
        if (dto.getEndLon() != null) route.setEndLon(dto.getEndLon());
        
        // Время отправления
        if (dto.getDepartureTime() != null) {
            route.setDepartureTime(dto.getDepartureTime());
        } else {
            route.setDepartureTime(LocalDateTime.now().plusHours(1));
        }
        
        // Связанные сущности
        if (dto.getVehicleId() != null) {
            vehicleService.findById(dto.getVehicleId()).ifPresent(route::setVehicle);
        }
        if (dto.getDriverId() != null) {
            driverService.findById(dto.getDriverId()).ifPresent(route::setDriver);
        }
        if (dto.getCargoId() != null) {
            cargoService.findById(dto.getCargoId()).ifPresent(route::setCargo);
        }
        
        // Генерируем все расчетные поля
        generateAllRouteData(route, dto);
        
        route = routeRepository.save(route);
        log.info("Создан маршрут с ID: {} и названием: '{}'", route.getId(), route.getName());
        
        // Создаем уведомление о создании маршрута
        notificationService.notifyRouteCreated(route.getId(), route.getName());
        
        // Если назначен водитель, создаем уведомление о назначении
        if (route.getDriver() != null) {
            String driverName = route.getDriver().getFirstName() + " " + route.getDriver().getLastName();
            notificationService.notifyDriverAssigned(route.getId(), route.getName(), driverName);
        }
        
        // Если назначен груз, создаем уведомление о назначении груза
        if (route.getCargo() != null) {
            notificationService.notifyCargoAssigned(route.getId(), route.getName(), route.getCargo().getName());
        }
        
        return findDetailById(route.getId());
    }

    @Transactional
    public RouteDetailDto updateRoute(Long id, RouteCreateUpdateDto dto) {
        Route existing = routeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Route", id));
        
        // Обновляем основные поля
        existing.setName(dto.getName());
        existing.setStartAddress(dto.getStartAddress());
        existing.setEndAddress(dto.getEndAddress());
        
        // Обновляем координаты (исправленные названия методов)
        if (dto.getStartLat() != null) existing.setStartLat(dto.getStartLat());
        if (dto.getStartLon() != null) existing.setStartLon(dto.getStartLon());
        if (dto.getEndLat() != null) existing.setEndLat(dto.getEndLat());
        if (dto.getEndLon() != null) existing.setEndLon(dto.getEndLon());
        
        // Обновляем время отправления
        if (dto.getDepartureTime() != null) {
            existing.setDepartureTime(dto.getDepartureTime());
        }
        
        // Обновляем связанные сущности
        if (dto.getVehicleId() != null) {
            vehicleService.findById(dto.getVehicleId()).ifPresent(existing::setVehicle);
        }
        if (dto.getDriverId() != null) {
            driverService.findById(dto.getDriverId()).ifPresent(existing::setDriver);
        }
        if (dto.getCargoId() != null) {
            cargoService.findById(dto.getCargoId()).ifPresent(existing::setCargo);
        }
        
        // Пересчитываем все данные
        generateAllRouteData(existing, dto);
        
        existing = routeRepository.save(existing);
        log.info("Обновлен маршрут с ID: {}", existing.getId());
        
        // Создаем уведомление об обновлении маршрута
        notificationService.notifyRouteUpdated(existing.getId(), existing.getName());
        
        return findDetailById(existing.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (!routeRepository.existsById(id)) {
            throw new EntityNotFoundException("Route", id);
        }
        routeRepository.deleteById(id);
        log.info("Удален маршрут с ID: {}", id);
    }

    /**
     * Генерирует все расчетные поля маршрута со случайными данными
     */
    private void generateAllRouteData(Route route, RouteCreateUpdateDto dto) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        
        // 1. РАССТОЯНИЕ И ВРЕМЯ
        generateDistanceAndTime(route, dto, rnd);
        
        // 2. РАСХОД ТОПЛИВА
        generateFuelConsumption(route, rnd);
        
        // 3. ЭКОНОМИЧЕСКИЕ ПОКАЗАТЕЛИ
        generateEconomicData(route, rnd);
        
        // 4. ОЦЕНКИ РИСКОВ
        generateRiskScores(route, rnd);
        
        // 5. СТАТУС И ДРУГИЕ ПОЛЯ
        setDefaultFields(route);
        
        log.info("Сгенерированы данные для маршрута '{}': {} км, {} мин, {} RUB", 
                route.getName(), 
                route.getDistanceKm(), 
                route.getEstimatedDurationMinutes(), 
                route.getEstimatedTotalCost());
    }
    
    private void generateDistanceAndTime(Route route, RouteCreateUpdateDto dto, ThreadLocalRandom rnd) {
        // Расчет расстояния
        if (dto.getStartLat() != null && dto.getStartLon() != null && 
            dto.getEndLat() != null && dto.getEndLon() != null) {
            double distance = calculateHaversineDistance(
                dto.getStartLat(), dto.getStartLon(), 
                dto.getEndLat(), dto.getEndLon()
            );
            route.setDistanceKm(BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP));
        } else {
            // Случайное расстояние если координаты не заданы
            route.setDistanceKm(BigDecimal.valueOf(rnd.nextDouble(50, 1200)).setScale(2, RoundingMode.HALF_UP));
        }
        
        // Расчет времени в пути (базово ~60 км/ч + вариации)
        double baseSpeed = rnd.nextDouble(45, 75); // км/ч
        int estimatedMinutes = (int) (route.getDistanceKm().doubleValue() * 60 / baseSpeed);
        int variation = rnd.nextInt(-15, 16); // ±15%
        int finalDuration = Math.max(15, estimatedMinutes + (estimatedMinutes * variation / 100));
        
        route.setEstimatedDurationMinutes(finalDuration);
        
        // Время прибытия
        if (route.getDepartureTime() != null) {
            route.setEstimatedArrivalTime(route.getDepartureTime().plusMinutes(finalDuration));
        }
    }
    
    private void generateFuelConsumption(Route route, ThreadLocalRandom rnd) {
        // Расход топлива зависит от расстояния (примерно 25-35 л/100км)
        double fuelPer100km = rnd.nextDouble(25, 35);
        BigDecimal estimatedFuel = route.getDistanceKm()
                .multiply(BigDecimal.valueOf(fuelPer100km / 100))
                .setScale(2, RoundingMode.HALF_UP);
        
        route.setEstimatedFuelConsumption(estimatedFuel);
        
        // Фактический расход с отклонением ±15%
        route.setActualFuelConsumption(
            estimatedFuel.multiply(BigDecimal.valueOf(rnd.nextDouble(0.85, 1.15)))
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }
    
    private void generateEconomicData(Route route, ThreadLocalRandom rnd) {
        // Стоимость топлива (примерно 50-60 руб/литр)
        double fuelPricePerLiter = rnd.nextDouble(50, 60);
        BigDecimal fuelCost = route.getEstimatedFuelConsumption()
                .multiply(BigDecimal.valueOf(fuelPricePerLiter))
                .setScale(2, RoundingMode.HALF_UP);
        route.setEstimatedFuelCost(fuelCost);
        
        // Дорожные сборы (0-15 руб/км)
        BigDecimal tollRate = BigDecimal.valueOf(rnd.nextDouble(0, 15));
        BigDecimal tollCost = route.getDistanceKm()
                .multiply(tollRate)
                .setScale(2, RoundingMode.HALF_UP);
        route.setEstimatedTollCost(tollCost);
        
        // Стоимость водителя (зависит от времени: 300-500 руб/час)
        double hourlyRate = rnd.nextDouble(300, 500);
        double hours = route.getEstimatedDurationMinutes() / 60.0;
        BigDecimal driverCost = BigDecimal.valueOf(hours * hourlyRate)
                .setScale(2, RoundingMode.HALF_UP);
        route.setEstimatedDriverCost(driverCost);
        
        // Общие затраты
        BigDecimal totalCost = fuelCost.add(tollCost).add(driverCost);
        route.setEstimatedTotalCost(totalCost);
        
        // Фактические затраты с отклонением ±10%
        route.setActualTotalCost(
            totalCost.multiply(BigDecimal.valueOf(rnd.nextDouble(0.9, 1.1)))
                    .setScale(2, RoundingMode.HALF_UP)
        );
    }
    
    private void generateRiskScores(Route route, ThreadLocalRandom rnd) {
        // Все риски в диапазоне 0-100
        route.setWeatherRiskScore(BigDecimal.valueOf(rnd.nextInt(0, 101)));
        route.setRoadQualityRiskScore(BigDecimal.valueOf(rnd.nextInt(0, 101)));
        route.setTrafficRiskScore(BigDecimal.valueOf(rnd.nextInt(0, 101)));
        route.setCargoRiskScore(BigDecimal.valueOf(rnd.nextInt(0, 101)));
        
        // Общий риск как среднее арифметическое других рисков
        BigDecimal avgRisk = route.getWeatherRiskScore()
                .add(route.getRoadQualityRiskScore())
                .add(route.getTrafficRiskScore())
                .add(route.getCargoRiskScore())
                .divide(BigDecimal.valueOf(4), RoundingMode.HALF_UP);
        
        route.setOverallRiskScore(avgRisk);
    }
    
    private void setDefaultFields(Route route) {
        if (route.getStatus() == null) {
            route.setStatus(Route.RouteStatus.PLANNED);
        }
        if (route.getCurrency() == null) {
            route.setCurrency("RUB");
        }
    }
    
    /**
     * Вычисляет расстояние между двумя точками по формуле Haversine
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Радиус Земли в км
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // расстояние в километрах
    }
}
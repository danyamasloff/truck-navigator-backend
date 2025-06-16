package ru.maslov.trucknavigator.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.driver.DriverPerformanceDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.repository.DriverRepository;
import ru.maslov.trucknavigator.repository.RouteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverPerformanceService {

    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;

    /**
     * Анализирует эффективность работы водителя за указанный период.
     *
     * @param driverId ID водителя
     * @param startDate начало периода (опционально)
     * @param endDate конец периода (опционально)
     * @return DTO с показателями эффективности
     */
    public DriverPerformanceDto analyzeDriverPerformance(
            Long driverId, LocalDateTime startDate, LocalDateTime endDate) {

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        // Устанавливаем значения по умолчанию, если даты не указаны
        LocalDateTime effectiveStartDate = startDate != null ?
                startDate : LocalDateTime.now().minusMonths(3);

        LocalDateTime effectiveEndDate = endDate != null ?
                endDate : LocalDateTime.now();

        // Получаем маршруты водителя за указанный период
        List<Route> driverRoutes = routeRepository
                .findRoutesInTimeRange(effectiveStartDate, effectiveEndDate).stream()
                .filter(route -> route.getDriver() != null &&
                        route.getDriver().getId().equals(driverId))
                .toList();

        // Собираем данные о производительности
        int completedRoutes = (int) driverRoutes.stream()
                .filter(route -> Route.RouteStatus.COMPLETED.equals(route.getStatus()))
                .count();

        BigDecimal totalDistance = driverRoutes.stream()
                .map(Route::getDistanceKm)
                .filter(distance -> distance != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Рассчитываем средние показатели (если доступны)
        BigDecimal avgFuelEfficiency = calculateAvgFuelEfficiency(driverRoutes);
        BigDecimal avgTimeEfficiency = calculateAvgTimeEfficiency(driverRoutes);

        // История показателей по месяцам
        List<DriverPerformanceDto.PerformanceHistoryPoint> history =
                generatePerformanceHistory(driverRoutes);

        // Формируем результат
        return DriverPerformanceDto.builder()
                .driverId(driverId)
                .driverName(driver.getLastName() + " " + driver.getFirstName())
                .analyzedPeriodStart(effectiveStartDate)
                .analyzedPeriodEnd(effectiveEndDate)
                .completedRoutesCount(completedRoutes)
                .totalDistanceDrivenKm(totalDistance)
                .avgFuelEfficiencyPercent(avgFuelEfficiency)
                .avgDeliveryTimeEfficiencyPercent(avgTimeEfficiency)
                .rating(driver.getRating())
                .incidentsCount(driver.getIncidentsCount())
                .performanceHistory(history)
                .build();
    }

    /**
     * Рассчитывает среднюю эффективность расхода топлива.
     */
    private BigDecimal calculateAvgFuelEfficiency(List<Route> routes) {
        if (routes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long routesWithFuelData = routes.stream()
                .filter(route -> route.getEstimatedFuelConsumption() != null &&
                        route.getActualFuelConsumption() != null)
                .count();

        if (routesWithFuelData == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalEfficiency = routes.stream()
                .filter(route -> route.getEstimatedFuelConsumption() != null &&
                        route.getActualFuelConsumption() != null)
                .map(route -> {
                    BigDecimal estimatedConsumption = route.getEstimatedFuelConsumption();
                    BigDecimal actualConsumption = route.getActualFuelConsumption();

                    if (estimatedConsumption.compareTo(BigDecimal.ZERO) == 0) {
                        return BigDecimal.ZERO;
                    }

                    // Эффективность = 100 * (estimated / actual)
                    // Если actual меньше estimated, эффективность > 100%
                    return new BigDecimal(100)
                            .multiply(estimatedConsumption)
                            .divide(actualConsumption, 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalEfficiency.divide(BigDecimal.valueOf(routesWithFuelData), 2, RoundingMode.HALF_UP);
    }

    /**
     * Рассчитывает среднюю эффективность по времени доставки.
     */
    private BigDecimal calculateAvgTimeEfficiency(List<Route> routes) {
        if (routes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long routesWithTimeData = routes.stream()
                .filter(route -> route.getEstimatedDurationMinutes() != null &&
                        route.getDepartureTime() != null &&
                        route.getActualArrivalTime() != null)
                .count();

        if (routesWithTimeData == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalEfficiency = routes.stream()
                .filter(route -> route.getEstimatedDurationMinutes() != null &&
                        route.getDepartureTime() != null &&
                        route.getActualArrivalTime() != null)
                .map(route -> {
                    int estimatedMinutes = route.getEstimatedDurationMinutes();
                    LocalDateTime departure = route.getDepartureTime();
                    LocalDateTime arrival = route.getActualArrivalTime();

                    // Фактическое время в пути в минутах
                    long actualMinutes = java.time.Duration.between(departure, arrival).toMinutes();

                    if (actualMinutes <= 0) {
                        return BigDecimal.ZERO;
                    }

                    // Эффективность = 100 * (estimated / actual)
                    return new BigDecimal(100)
                            .multiply(BigDecimal.valueOf(estimatedMinutes))
                            .divide(BigDecimal.valueOf(actualMinutes), 2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalEfficiency.divide(BigDecimal.valueOf(routesWithTimeData), 2, RoundingMode.HALF_UP);
    }

    /**
     * Генерирует историю показателей производительности.
     */
    private List<DriverPerformanceDto.PerformanceHistoryPoint> generatePerformanceHistory(List<Route> routes) {
        // Здесь должна быть логика группировки маршрутов по месяцам и расчета показателей
        // Для примера возвращаем пустой список
        return new ArrayList<>();
    }
}

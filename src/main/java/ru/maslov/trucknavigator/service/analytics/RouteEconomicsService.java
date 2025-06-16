package ru.maslov.trucknavigator.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.maslov.trucknavigator.dto.analytics.*;
import ru.maslov.trucknavigator.entity.*;
import ru.maslov.trucknavigator.repository.CostParametersRepository;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сервис для анализа экономических показателей маршрутов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteEconomicsService {

    private final CostParametersRepository costParametersRepository;

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Анализирует экономические показатели маршрута.
     *
     * @param route маршрут для анализа
     * @param vehicle транспортное средство
     * @param driver водитель
     * @param cargo груз
     * @return DTO с результатами анализа
     */
    @Transactional(readOnly = true)
    public RouteEconomicsDto analyzeRouteEconomics(Route route, Vehicle vehicle, Driver driver, Cargo cargo) {
        // Находим актуальные параметры стоимости
        CostParameters costParams = findActiveCostParameters(route.getStartAddress());

        // Расчет расхода топлива
        FuelConsumptionAnalysisDto fuelAnalysis = calculateFuelConsumption(route, vehicle, cargo);

        // Расчет затрат на топливо
        BigDecimal fuelCost = calculateFuelCost(fuelAnalysis, costParams);

        // Расчет затрат на водителя
        BigDecimal driverCost = calculateDriverCost(route, driver, costParams);

        // Расчет затрат на платные дороги
        BigDecimal tollRoadsCost = calculateTollRoadsCost(route, costParams);

        // Расчет амортизации и затрат на обслуживание
        BigDecimal vehicleDepreciation = calculateVehicleDepreciation(route, vehicle, costParams);
        BigDecimal maintenanceCost = calculateMaintenanceCost(route, vehicle, costParams);

        // Расчет общей стоимости
        BigDecimal totalCost = fuelCost
                .add(driverCost)
                .add(tollRoadsCost)
                .add(vehicleDepreciation)
                .add(maintenanceCost);

        // Расчет стоимости на километр
        BigDecimal costPerKm = BigDecimal.ZERO;
        if (route.getDistanceKm() != null && route.getDistanceKm().compareTo(BigDecimal.ZERO) > 0) {
            costPerKm = totalCost.divide(route.getDistanceKm(), SCALE, ROUNDING_MODE);
        }

        return RouteEconomicsDto.builder()
                .routeId(route.getId())
                .fuelCost(fuelCost)
                .driverCost(driverCost)
                .tollRoadsCost(tollRoadsCost)
                .vehicleDepreciation(vehicleDepreciation)
                .maintenanceCost(maintenanceCost)
                .totalCost(totalCost)
                .costPerKm(costPerKm)
                .currency(costParams.getCurrency())
                .calculationTime(LocalDateTime.now())
                .build();
    }

    /**
     * Рассчитывает расход топлива для маршрута.
     *
     * @param route маршрут
     * @param vehicle транспортное средство
     * @param cargo груз
     * @return DTO с результатами анализа расхода топлива
     */
    private FuelConsumptionAnalysisDto calculateFuelConsumption(Route route, Vehicle vehicle, Cargo cargo) {
        BigDecimal baseConsumption = vehicle.getFuelConsumptionPer100km();
        if (baseConsumption == null) {
            baseConsumption = BigDecimal.valueOf(30.0); // Значение по умолчанию для грузовика
        }

        BigDecimal totalDistance = route.getDistanceKm();
        if (totalDistance == null) {
            totalDistance = BigDecimal.ZERO;
        }

        // Факторы влияния на расход
        BigDecimal cargoWeightFactor = calculateCargoWeightFactor(vehicle, cargo);
        BigDecimal weatherFactor = calculateWeatherFactor(route);
        BigDecimal roadSlopeFactor = calculateRoadSlopeFactor(route);

        // Расчет скорректированного расхода
        BigDecimal adjustedConsumption = baseConsumption
                .multiply(cargoWeightFactor)
                .multiply(weatherFactor)
                .multiply(roadSlopeFactor)
                .setScale(SCALE, ROUNDING_MODE);

        // Общий расход топлива
        BigDecimal totalConsumption = totalDistance
                .multiply(adjustedConsumption)
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);

        StringBuilder explanation = new StringBuilder();
        explanation.append("Базовый расход: ").append(baseConsumption).append(" л/100км. ");

        if (cargoWeightFactor.compareTo(BigDecimal.ONE) != 0) {
            explanation.append("Влияние веса груза: +").append(cargoWeightFactor.subtract(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(100)).setScale(0, ROUNDING_MODE)).append("%. ");
        }

        if (weatherFactor.compareTo(BigDecimal.ONE) != 0) {
            explanation.append("Влияние погоды: +").append(weatherFactor.subtract(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(100)).setScale(0, ROUNDING_MODE)).append("%. ");
        }

        if (roadSlopeFactor.compareTo(BigDecimal.ONE) != 0) {
            explanation.append("Влияние рельефа: +").append(roadSlopeFactor.subtract(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(100)).setScale(0, ROUNDING_MODE)).append("%. ");
        }

        explanation.append("Итоговый расход: ").append(adjustedConsumption).append(" л/100км.");

        return FuelConsumptionAnalysisDto.builder()
                .baseConsumptionPer100Km(baseConsumption)
                .adjustedConsumptionPer100Km(adjustedConsumption)
                .totalFuelConsumption(totalConsumption)
                .cargoWeightImpact(cargoWeightFactor)
                .weatherImpact(weatherFactor)
                .roadSlopeImpact(roadSlopeFactor)
                .totalDistance(totalDistance)
                .analysisExplanation(explanation.toString())
                .build();
    }

    /**
     * Вычисляет влияние веса груза на расход топлива.
     *
     * @param vehicle транспортное средство
     * @param cargo груз
     * @return коэффициент влияния
     */
    private BigDecimal calculateCargoWeightFactor(Vehicle vehicle, Cargo cargo) {
        if (cargo == null || cargo.getWeightKg() == null || vehicle.getMaxLoadCapacityKg() == null
                || vehicle.getMaxLoadCapacityKg() == 0) {
            return BigDecimal.ONE;
        }

        // Степень загрузки ТС (0.0 - 1.0)
        double loadFactor = Math.min(1.0, cargo.getWeightKg().doubleValue() / vehicle.getMaxLoadCapacityKg());

        // При полной загрузке расход увеличивается до 30%
        return BigDecimal.valueOf(1.0 + (0.3 * loadFactor)).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Вычисляет влияние погоды на расход топлива.
     *
     * @param route маршрут
     * @return коэффициент влияния
     */
    private BigDecimal calculateWeatherFactor(Route route) {
        // Здесь должна быть логика получения погодных условий на маршруте
        // Для простоты используем фиксированное значение
        return BigDecimal.valueOf(1.05).setScale(SCALE, ROUNDING_MODE); // +5% на погодные условия
    }

    /**
     * Вычисляет влияние рельефа дороги на расход топлива.
     *
     * @param route маршрут
     * @return коэффициент влияния
     */
    private BigDecimal calculateRoadSlopeFactor(Route route) {
        // Здесь должна быть логика анализа рельефа на маршруте
        // Для простоты используем фиксированное значение
        return BigDecimal.valueOf(1.07).setScale(SCALE, ROUNDING_MODE); // +7% на рельеф
    }

    /**
     * Рассчитывает стоимость топлива.
     *
     * @param fuelAnalysis результаты анализа расхода топлива
     * @param costParams параметры стоимости
     * @return стоимость топлива
     */
    private BigDecimal calculateFuelCost(FuelConsumptionAnalysisDto fuelAnalysis, CostParameters costParams) {
        BigDecimal fuelPrice = costParams.getFuelPricePerLiter();

        if (fuelPrice == null || fuelPrice.compareTo(BigDecimal.ZERO) <= 0) {
            fuelPrice = BigDecimal.valueOf(50.0); // Значение по умолчанию
        }

        return fuelAnalysis.getTotalFuelConsumption()
                .multiply(fuelPrice)
                .setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Рассчитывает затраты на водителя.
     *
     * @param route маршрут
     * @param driver водитель
     * @param costParams параметры стоимости
     * @return затраты на водителя
     */
    private BigDecimal calculateDriverCost(Route route, Driver driver, CostParameters costParams) {
        if (driver == null || route.getEstimatedDurationMinutes() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal hourlyRate = driver.getHourlyRate();
        BigDecimal perKmRate = driver.getPerKilometerRate();

        // Если ставки водителя не указаны, используем значения из параметров
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            hourlyRate = costParams.getDriverCostPerHour();
        }

        if (perKmRate == null || perKmRate.compareTo(BigDecimal.ZERO) <= 0) {
            perKmRate = costParams.getDriverCostPerKm();
        }

        // Если все равно нет данных, используем значения по умолчанию
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            hourlyRate = BigDecimal.valueOf(300.0); // 300 руб/час
        }

        if (perKmRate == null || perKmRate.compareTo(BigDecimal.ZERO) <= 0) {
            perKmRate = BigDecimal.valueOf(10.0); // 10 руб/км
        }

        // Расчет стоимости по времени
        BigDecimal durationHours = BigDecimal.valueOf(route.getEstimatedDurationMinutes())
                .divide(BigDecimal.valueOf(60), SCALE, ROUNDING_MODE);
        BigDecimal timeCost = durationHours.multiply(hourlyRate);

        // Расчет стоимости по километражу
        BigDecimal distanceCost = BigDecimal.ZERO;
        if (route.getDistanceKm() != null) {
            distanceCost = route.getDistanceKm().multiply(perKmRate);
        }

        return timeCost.add(distanceCost).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Рассчитывает затраты на платные дороги.
     *
     * @param route маршрут
     * @param costParams параметры стоимости
     * @return затраты на платные дороги
     */
    private BigDecimal calculateTollRoadsCost(Route route, CostParameters costParams) {
        // Для демонстрации, реальная логика должна учитывать фактические платные участки
        BigDecimal tollCostPerKm = costParams.getTollRoadAverageCostPerKm();

        if (tollCostPerKm == null || tollCostPerKm.compareTo(BigDecimal.ZERO) <= 0) {
            tollCostPerKm = BigDecimal.valueOf(3.0); // 3 руб/км
        }

        // Предполагаем, что 20% маршрута - платные дороги
        BigDecimal tollRoadDistance = BigDecimal.ZERO;
        if (route.getDistanceKm() != null) {
            tollRoadDistance = route.getDistanceKm().multiply(BigDecimal.valueOf(0.2));
        }

        return tollRoadDistance.multiply(tollCostPerKm).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Рассчитывает затраты на амортизацию ТС.
     *
     * @param route маршрут
     * @param vehicle транспортное средство
     * @param costParams параметры стоимости
     * @return затраты на амортизацию
     */
    private BigDecimal calculateVehicleDepreciation(Route route, Vehicle vehicle, CostParameters costParams) {
        BigDecimal depreciationPerKm = vehicle.getVehicleDepreciationPerKm();

        if (depreciationPerKm == null || depreciationPerKm.compareTo(BigDecimal.ZERO) <= 0) {
            depreciationPerKm = costParams.getVehicleDepreciationPerKm();
        }

        if (depreciationPerKm == null || depreciationPerKm.compareTo(BigDecimal.ZERO) <= 0) {
            depreciationPerKm = BigDecimal.valueOf(5.0); // 5 руб/км
        }

        BigDecimal totalDistance = BigDecimal.ZERO;
        if (route.getDistanceKm() != null) {
            totalDistance = route.getDistanceKm();
        }

        return totalDistance.multiply(depreciationPerKm).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Рассчитывает затраты на техническое обслуживание ТС.
     *
     * @param route маршрут
     * @param vehicle транспортное средство
     * @param costParams параметры стоимости
     * @return затраты на техническое обслуживание
     */
    private BigDecimal calculateMaintenanceCost(Route route, Vehicle vehicle, CostParameters costParams) {
        BigDecimal maintenanceCostPerKm = vehicle.getAvgMaintenanceCostPerKm();

        if (maintenanceCostPerKm == null || maintenanceCostPerKm.compareTo(BigDecimal.ZERO) <= 0) {
            maintenanceCostPerKm = costParams.getMaintenanceCostPerKm();
        }

        if (maintenanceCostPerKm == null || maintenanceCostPerKm.compareTo(BigDecimal.ZERO) <= 0) {
            maintenanceCostPerKm = BigDecimal.valueOf(2.0); // 2 руб/км
        }

        BigDecimal totalDistance = BigDecimal.ZERO;
        if (route.getDistanceKm() != null) {
            totalDistance = route.getDistanceKm();
        }

        return totalDistance.multiply(maintenanceCostPerKm).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Находит актуальные параметры стоимости для указанного региона.
     *
     * @param regionCode код региона (или адрес маршрута для определения региона)
     * @return параметры стоимости
     */
    private CostParameters findActiveCostParameters(String regionCode) {
        LocalDate now = LocalDate.now();
        String region = extractRegionCode(regionCode);

        // Ищем параметры для указанного региона
        if (region != null && !region.isEmpty()) {
            return costParametersRepository.findActiveByDateAndRegion(now, region)
                    .orElse(
                            // Если не найдены параметры для региона, используем общие параметры
                            costParametersRepository.findActiveByDate(now)
                                    .orElseThrow(() -> new EntityNotFoundException(
                                            "Active cost parameters not found"))
                    );
        }

        // Если регион не указан, используем общие параметры
        return costParametersRepository.findActiveByDate(now)
                .orElseThrow(() -> new EntityNotFoundException("Active cost parameters not found"));
    }

    /**
     * Извлекает код региона из адреса.
     *
     * @param address адрес
     * @return код региона
     */
    private String extractRegionCode(String address) {
        // В реальном приложении здесь должна быть логика определения региона по адресу
        // Для простоты возвращаем фиксированное значение
        return "RU-77"; // Москва
    }
}

package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.fuel.FuelPriceResponse;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.integration.fuelprice.FuelPriceService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Сервис для расчета экономических показателей маршрута.
 * Координирует работу различных сервисов для получения полной экономической картины.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteEconomicsCalculatorService {

    private final FuelPriceService fuelPriceService;

    @Value("${profitability.fuel.cost.weight:0.5}")
    private double fuelCostWeight;

    @Value("${profitability.toll.roads.weight:0.2}")
    private double tollRoadsWeight;

    @Value("${profitability.vehicle.wear.weight:0.2}")
    private double vehicleWearWeight;

    @Value("${profitability.driver.time.weight:0.1}")
    private double driverTimeWeight;

    // Базовые экономические параметры
    private static final BigDecimal DEFAULT_FUEL_PRICE = new BigDecimal("65.00");
    private static final BigDecimal TOLL_COST_PER_KM = new BigDecimal("2.50"); // средняя стоимость платных дорог
    private static final BigDecimal VEHICLE_WEAR_COST_PER_KM = new BigDecimal("8.00"); // износ ТС

    /**
     * Рассчитывает все экономические показатели для маршрута.
     *
     * @param route построенный маршрут
     * @param vehicle транспортное средство
     * @param driver водитель (может быть null)
     * @return обновленный маршрут с экономическими данными
     */
    public RouteResponseDto calculateFullEconomics(RouteResponseDto route, Vehicle vehicle, Driver driver) {
        log.info("Начинаем расчет экономических показателей для маршрута длиной {} км", route.getDistance());

        try {
            // 1. Расчет расхода топлива
            calculateAdvancedFuelConsumption(route, vehicle);

            // 2. Расчет стоимости топлива с учетом маршрута
            calculateOptimalFuelCost(route);

            // 3. Расчет затрат на платные дороги
            calculateTollRoadCosts(route);

            // 4. Расчет затрат на водителя
            if (driver != null) {
                calculateDriverCompensation(route, driver);
            }

            // 5. Расчет общей стоимости поездки
            calculateTotalTripCost(route);

            log.info("Расчет экономических показателей завершен. Общая стоимость: {} руб.", 
                    route.getEstimatedTotalCost());

        } catch (Exception e) {
            log.error("Ошибка при расчете экономических показателей: {}", e.getMessage(), e);
            // Устанавливаем базовые значения при ошибке
            setFallbackEconomics(route, vehicle);
        }

        return route;
    }

    /**
     * Расширенный расчет расхода топлива с учетом множества факторов.
     */
    private void calculateAdvancedFuelConsumption(RouteResponseDto route, Vehicle vehicle) {
        BigDecimal baseFuelConsumption = vehicle.getFuelConsumptionPer100km();
        if (baseFuelConsumption == null || baseFuelConsumption.compareTo(BigDecimal.ZERO) <= 0) {
            baseFuelConsumption = determineDefaultFuelConsumption(vehicle);
        }

        BigDecimal distance = route.getDistance();
        if (distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Некорректное расстояние маршрута: {}", distance);
            return;
        }

        // Факторы влияния на расход топлива
        BigDecimal weatherFactor = calculateWeatherImpact(route);
        BigDecimal roadQualityFactor = calculateRoadQualityImpact(route);
        BigDecimal trafficFactor = calculateTrafficImpact(route);
        BigDecimal terrainFactor = calculateTerrainImpact(route);

        // Общий корректирующий коэффициент
        BigDecimal totalFactor = BigDecimal.ONE
                .add(weatherFactor.multiply(new BigDecimal("0.25")))
                .add(roadQualityFactor.multiply(new BigDecimal("0.30")))
                .add(trafficFactor.multiply(new BigDecimal("0.25")))
                .add(terrainFactor.multiply(new BigDecimal("0.20")));

        // Скорректированный расход топлива
        BigDecimal adjustedConsumption = baseFuelConsumption.multiply(totalFactor);

        // Общий расход топлива
        BigDecimal totalConsumption = adjustedConsumption
                .multiply(distance)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        route.setEstimatedFuelConsumption(totalConsumption);

        log.debug("Расход топлива: базовый={} л/100км, скорректированный={} л/100км, общий={} л", 
                baseFuelConsumption, adjustedConsumption, totalConsumption);
    }

    /**
     * Оптимизированный расчет стоимости топлива с учетом цен по маршруту.
     */
    private void calculateOptimalFuelCost(RouteResponseDto route) {
        if (route.getEstimatedFuelConsumption() == null) {
            log.warn("Невозможно рассчитать стоимость топлива: отсутствует расход топлива");
            return;
        }

        try {
            // Получаем оптимальную цену топлива по маршруту
            BigDecimal optimalFuelPrice = getOptimalFuelPrice(route);
            
            BigDecimal fuelCost = route.getEstimatedFuelConsumption()
                    .multiply(optimalFuelPrice)
                    .setScale(2, RoundingMode.HALF_UP);

            route.setEstimatedFuelCost(fuelCost);

            log.debug("Стоимость топлива: {} л × {} руб/л = {} руб", 
                    route.getEstimatedFuelConsumption(), optimalFuelPrice, fuelCost);

        } catch (Exception e) {
            log.warn("Ошибка при расчете стоимости топлива: {}", e.getMessage());
            // Используем стандартную цену
            BigDecimal fuelCost = route.getEstimatedFuelConsumption()
                    .multiply(DEFAULT_FUEL_PRICE)
                    .setScale(2, RoundingMode.HALF_UP);
            route.setEstimatedFuelCost(fuelCost);
        }
    }

    /**
     * Получает оптимальную цену топлива по маршруту.
     */
    private BigDecimal getOptimalFuelPrice(RouteResponseDto route) {
        if (route.getCoordinates() == null || route.getCoordinates().isEmpty()) {
            return DEFAULT_FUEL_PRICE;
        }

        try {
            // Получаем координаты начала и конца маршрута
            List<double[]> coordinates = route.getCoordinates();
            double[] startPoint = coordinates.get(0);
            double[] endPoint = coordinates.get(coordinates.size() - 1);

            FuelPriceResponse fuelPrices = fuelPriceService.getRouteFuelPrices(
                    startPoint[1], startPoint[0], // lat, lon
                    endPoint[1], endPoint[0],     // lat, lon
                    "DIESEL"
            );

            if (fuelPrices.isSuccess() && fuelPrices.getMinPrice() != null) {
                log.debug("Получена оптимальная цена топлива: {} руб/л (мин: {}, средняя: {})", 
                        fuelPrices.getMinPrice(), fuelPrices.getMinPrice(), fuelPrices.getAveragePrice());
                return fuelPrices.getMinPrice();
            }

        } catch (Exception e) {
            log.warn("Не удалось получить цены топлива по маршруту: {}", e.getMessage());
        }

        return DEFAULT_FUEL_PRICE;
    }

    /**
     * Расчет затрат на платные дороги.
     */
    private void calculateTollRoadCosts(RouteResponseDto route) {
        // Пока используем упрощенный расчет
        // В будущем здесь будет интеграция с API платных дорог
        BigDecimal distance = route.getDistance();
        if (distance == null) {
            return;
        }

        // Примерно 30% дорог могут быть платными
        BigDecimal tollDistance = distance.multiply(new BigDecimal("0.3"));
        BigDecimal tollCost = tollDistance.multiply(TOLL_COST_PER_KM)
                .setScale(2, RoundingMode.HALF_UP);

        route.setEstimatedTollCost(tollCost);

        log.debug("Затраты на платные дороги: {} км × {} руб/км = {} руб", 
                tollDistance, TOLL_COST_PER_KM, tollCost);
    }

    /**
     * Расчет компенсации водителю.
     */
    private void calculateDriverCompensation(RouteResponseDto route, Driver driver) {
        BigDecimal driverCost = BigDecimal.ZERO;

        // Расчет по часовой ставке
        if (driver.getHourlyRate() != null && route.getDuration() > 0) {
            BigDecimal hours = BigDecimal.valueOf(route.getDuration())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            driverCost = driver.getHourlyRate().multiply(hours);
        }

        // Дополнительные расходы (суточные, если поездка длинная)
        if (route.getDuration() > 8 * 60) { // больше 8 часов
            BigDecimal dailyAllowance = new BigDecimal("1500.00"); // суточные
            driverCost = driverCost.add(dailyAllowance);
        }

        route.setEstimatedDriverCost(driverCost.setScale(2, RoundingMode.HALF_UP));

        log.debug("Затраты на водителя: {} руб (время: {} мин)", driverCost, route.getDuration());
    }

    /**
     * Расчет общей стоимости поездки.
     */
    private void calculateTotalTripCost(RouteResponseDto route) {
        BigDecimal totalCost = BigDecimal.ZERO;

        if (route.getEstimatedFuelCost() != null) {
            totalCost = totalCost.add(route.getEstimatedFuelCost());
        }

        if (route.getEstimatedTollCost() != null) {
            totalCost = totalCost.add(route.getEstimatedTollCost());
        }

        if (route.getEstimatedDriverCost() != null) {
            totalCost = totalCost.add(route.getEstimatedDriverCost());
        }

        // Добавляем износ транспортного средства
        if (route.getDistance() != null) {
            BigDecimal wearCost = route.getDistance().multiply(VEHICLE_WEAR_COST_PER_KM);
            totalCost = totalCost.add(wearCost);
        }

        route.setEstimatedTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Определяет базовый расход топлива по умолчанию в зависимости от типа ТС.
     */
    private BigDecimal determineDefaultFuelConsumption(Vehicle vehicle) {
        // Здесь можно добавить логику в зависимости от типа ТС
        // Пока используем стандартное значение для грузовика
        return new BigDecimal("35.0");
    }

    /**
     * Рассчитывает влияние погоды на расход топлива.
     */
    private BigDecimal calculateWeatherImpact(RouteResponseDto route) {
        if (route.getWeatherRiskScore() != null) {
            return route.getWeatherRiskScore()
                    .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.2")); // максимум 20% увеличения
        }
        return new BigDecimal("0.02"); // 2% по умолчанию
    }

    /**
     * Рассчитывает влияние качества дорог на расход топлива.
     */
    private BigDecimal calculateRoadQualityImpact(RouteResponseDto route) {
        if (route.getRoadQualityRiskScore() != null) {
            return route.getRoadQualityRiskScore()
                    .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.25")); // максимум 25% увеличения
        }
        return new BigDecimal("0.05"); // 5% по умолчанию
    }

    /**
     * Рассчитывает влияние трафика на расход топлива.
     */
    private BigDecimal calculateTrafficImpact(RouteResponseDto route) {
        if (route.getTrafficRiskScore() != null) {
            return route.getTrafficRiskScore()
                    .divide(new BigDecimal("100"), 3, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.15")); // максимум 15% увеличения
        }
        return new BigDecimal("0.03"); // 3% по умолчанию
    }

    /**
     * Рассчитывает влияние рельефа на расход топлива.
     */
    private BigDecimal calculateTerrainImpact(RouteResponseDto route) {
        // Простая оценка: если маршрут длинный, вероятно есть перепады высот
        if (route.getDistance() != null && route.getDistance().compareTo(new BigDecimal("300")) > 0) {
            return new BigDecimal("0.10"); // 10% для длинных маршрутов
        }
        return new BigDecimal("0.05"); // 5% для коротких маршрутов
    }

    /**
     * Устанавливает базовые экономические показатели при ошибке расчета.
     */
    private void setFallbackEconomics(RouteResponseDto route, Vehicle vehicle) {
        if (route.getDistance() != null) {
            // Базовые расчеты
            BigDecimal distance = route.getDistance();
            BigDecimal fuelConsumption = distance.multiply(new BigDecimal("35.0"))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal fuelCost = fuelConsumption.multiply(DEFAULT_FUEL_PRICE);

            route.setEstimatedFuelConsumption(fuelConsumption);
            route.setEstimatedFuelCost(fuelCost);
            route.setEstimatedTollCost(distance.multiply(new BigDecimal("2.0")));
            route.setEstimatedTotalCost(fuelCost.add(route.getEstimatedTollCost()));

            log.warn("Использованы базовые экономические показатели вместо точного расчета");
        }
    }
} 

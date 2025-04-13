package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Сервис для расчета рентабельности маршрута.
 * Анализирует экономические аспекты маршрута, включая расходы на топливо,
 * платные дороги, износ ТС и оплату труда водителя.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitabilityService {

    // Весовые коэффициенты для расчета стоимости, настраиваются в конфигурации
    @Value("${profitability.fuel.cost.weight:0.5}")
    private double fuelCostWeight;

    @Value("${profitability.toll.roads.weight:0.2}")
    private double tollRoadsWeight;

    @Value("${profitability.vehicle.wear.weight:0.2}")
    private double vehicleWearWeight;

    @Value("${profitability.driver.time.weight:0.1}")
    private double driverTimeWeight;

    // Средняя цена на дизельное топливо (в рублях за литр)
    // В реальном приложении будет получаться из внешнего API
    private static final BigDecimal DEFAULT_FUEL_PRICE = new BigDecimal("65.00");

    /**
     * Рассчитывает экономические показатели для заданного маршрута.
     *
     * @param route построенный маршрут
     * @param vehicle транспортное средство
     * @param driver водитель (может быть null)
     * @return обновленный маршрут с данными о стоимости
     */
    public RouteResponseDto calculateEconomics(RouteResponseDto route, Vehicle vehicle, Driver driver) {
        if (route == null || vehicle == null) {
            log.warn("Невозможно рассчитать экономику: недостаточно данных");
            return route;
        }

        // Расчет расхода топлива
        calculateFuelConsumption(route, vehicle);

        // Расчет стоимости топлива
        calculateFuelCost(route);

        // Расчет затрат на платные дороги
        calculateTollCosts(route);

        // Расчет затрат на водителя
        if (driver != null) {
            calculateDriverCosts(route, driver);
        }

        // Расчет общей стоимости поездки
        calculateTotalCost(route);

        return route;
    }

    /**
     * Рассчитывает расход топлива для маршрута.
     *
     * @param route построенный маршрут
     * @param vehicle транспортное средство
     */
    private void calculateFuelConsumption(RouteResponseDto route, Vehicle vehicle) {
        // Базовый расход топлива автомобиля (литров на 100 км)
        BigDecimal baseFuelConsumption = vehicle.getFuelConsumptionPer100km();
        if (baseFuelConsumption == null || baseFuelConsumption.compareTo(BigDecimal.ZERO) <= 0) {
            // Если данные отсутствуют, используем усредненное значение для грузовиков
            baseFuelConsumption = new BigDecimal("35.0");
        }

        // Общая дистанция в километрах
        BigDecimal distance = route.getDistance();

        // Учитываем влияние различных факторов на расход
        BigDecimal weatherFactor = calculateWeatherInfluence(route);
        BigDecimal roadQualityFactor = calculateRoadQualityInfluence(route);
        BigDecimal trafficFactor = calculateTrafficInfluence(route);

        // Итоговый корректирующий коэффициент (сумма весов должна быть равна 1)
        BigDecimal totalFactor = BigDecimal.ONE
                .add(weatherFactor.multiply(new BigDecimal("0.3")))
                .add(roadQualityFactor.multiply(new BigDecimal("0.4")))
                .add(trafficFactor.multiply(new BigDecimal("0.3")));

        // Фактический расход с учетом корректирующих факторов
        BigDecimal adjustedConsumption = baseFuelConsumption.multiply(totalFactor);

        // Общий расход топлива = (расход на 100 км * дистанция) / 100
        BigDecimal totalConsumption = adjustedConsumption
                .multiply(distance)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        route.setEstimatedFuelConsumption(totalConsumption);
    }

    /**
     * Рассчитывает влияние погодных условий на расход топлива.
     *
     * @param route построенный маршрут
     * @return коэффициент влияния (0.0 - 0.3)
     */
    private BigDecimal calculateWeatherInfluence(RouteResponseDto route) {
        // Если есть данные о погодных рисках, используем их
        if (route.getWeatherRiskScore() != null) {
            // Преобразуем оценку риска (0-100) в коэффициент влияния (0.0-0.3)
            return route.getWeatherRiskScore()
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.3"));
        }

        // Если данных нет, используем небольшой дефолтный коэффициент
        return new BigDecimal("0.05");
    }

    /**
     * Рассчитывает влияние качества дорог на расход топлива.
     *
     * @param route построенный маршрут
     * @return коэффициент влияния (0.0 - 0.4)
     */
    private BigDecimal calculateRoadQualityInfluence(RouteResponseDto route) {
        // Если есть данные о рисках качества дорог, используем их
        if (route.getRoadQualityRiskScore() != null) {
            // Преобразуем оценку риска (0-100) в коэффициент влияния (0.0-0.4)
            return route.getRoadQualityRiskScore()
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.4"));
        }

        // Если данных нет, используем средний коэффициент
        return new BigDecimal("0.1");
    }

    /**
     * Рассчитывает влияние трафика на расход топлива.
     *
     * @param route построенный маршрут
     * @return коэффициент влияния (0.0 - 0.3)
     */
    private BigDecimal calculateTrafficInfluence(RouteResponseDto route) {
        // Если есть данные о трафике, используем их
        if (route.getTrafficRiskScore() != null) {
            // Преобразуем оценку риска (0-100) в коэффициент влияния (0.0-0.3)
            return route.getTrafficRiskScore()
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.3"));
        }

        // Если данных нет, возвращаем малый коэффициент
        return new BigDecimal("0.05");
    }

    /**
     * Рассчитывает затраты на топливо.
     *
     * @param route построенный маршрут
     */
    private void calculateFuelCost(RouteResponseDto route) {
        if (route.getEstimatedFuelConsumption() == null) {
            log.warn("Невозможно рассчитать стоимость топлива: отсутствует расход топлива");
            return;
        }

        // В реальном приложении цена топлива будет получаться из внешнего API
        // или базы данных на основе маршрута и локации заправок
        BigDecimal fuelPrice = getFuelPrice(route);

        // Стоимость топлива = расход * цена за литр
        BigDecimal fuelCost = route.getEstimatedFuelConsumption().multiply(fuelPrice)
                .setScale(2, RoundingMode.HALF_UP);

        route.setEstimatedFuelCost(fuelCost);
    }

    /**
     * Получает цену на топливо для маршрута.
     *
     * @param route маршрут
     * @return цена за литр топлива
     */
    private BigDecimal getFuelPrice(RouteResponseDto route) {
        // Заглушка: в реальном приложении здесь будет обращение к API цен на топливо
        // или расчет средней цены по маршруту на основе данных из БД

        // Берем базовую цену и добавляем случайное отклонение +/- 10%
        Random random = new Random();
        double variation = (random.nextDouble() * 0.2) - 0.1; // от -0.1 до 0.1

        return DEFAULT_FUEL_PRICE
                .multiply(BigDecimal.ONE.add(new BigDecimal(variation)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Рассчитывает затраты на платные дороги.
     *
     * @param route построенный маршрут
     */
    private void calculateTollCosts(RouteResponseDto route) {
        List<RouteResponseDto.TollSegment> tollSegments = new ArrayList<>();
        BigDecimal totalTollCost = BigDecimal.ZERO;

        // Заглушка: в реальном приложении здесь будет получение данных о платных дорогах
        // из внешнего API или базы данных

        // Для демонстрации генерируем несколько случайных платных участков
        Random random = new Random();
        int numTollSegments = random.nextInt(3); // 0-2 платных участка

        if (route.getCoordinates() != null && !route.getCoordinates().isEmpty()) {
            List<double[]> coordinates = route.getCoordinates();
            int totalPoints = coordinates.size();

            for (int i = 0; i < numTollSegments; i++) {
                // Генерируем случайный отрезок маршрута для платной дороги
                int segmentLength = totalPoints / (numTollSegments + 1);
                int startIndex = (i + 1) * segmentLength - segmentLength / 2;
                int endIndex = Math.min(startIndex + random.nextInt(segmentLength), totalPoints - 1);

                // Рассчитываем примерное расстояние сегмента
                BigDecimal segmentDistance = route.getDistance()
                        .multiply(new BigDecimal((double) (endIndex - startIndex) / totalPoints))
                        .setScale(2, RoundingMode.HALF_UP);

                // Генерируем случайную стоимость на основе длины
                BigDecimal cost = segmentDistance
                        .multiply(new BigDecimal(random.nextDouble() * 5 + 5)) // 5-10 руб/км
                        .setScale(2, RoundingMode.HALF_UP);

                RouteResponseDto.TollSegment segment = new RouteResponseDto.TollSegment();
                segment.setStartIndex(startIndex);
                segment.setEndIndex(endIndex);
                segment.setDistance(segmentDistance);
                segment.setTollName("Платный участок " + (i + 1));
                segment.setCost(cost);
                segment.setCurrency("RUB");

                tollSegments.add(segment);
                totalTollCost = totalTollCost.add(cost);
            }
        }

        route.setTollSegments(tollSegments);
        route.setEstimatedTollCost(totalTollCost);
    }

    /**
     * Рассчитывает затраты на оплату труда водителя.
     *
     * @param route построенный маршрут
     * @param driver водитель
     */
    private void calculateDriverCosts(RouteResponseDto route, Driver driver) {
        // Проверяем наличие данных о времени и водителе
        if (route.getDuration() <= 0 || driver == null) {
            log.warn("Невозможно рассчитать затраты на водителя: недостаточно данных");
            return;
        }

        // Получаем ставки водителя
        BigDecimal hourlyRate = driver.getHourlyRate();
        BigDecimal perKmRate = driver.getPerKilometerRate();

        BigDecimal driverCost = BigDecimal.ZERO;

        // Расчет по почасовой ставке
        if (hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) > 0) {
            // Время в часах = время в минутах / 60
            BigDecimal hours = new BigDecimal(route.getDuration())
                    .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);

            driverCost = driverCost.add(hours.multiply(hourlyRate));
        }

        // Расчет по километровой ставке
        if (perKmRate != null && perKmRate.compareTo(BigDecimal.ZERO) > 0) {
            driverCost = driverCost.add(route.getDistance().multiply(perKmRate));
        }

        // Если обе ставки равны нулю, используем стандартную ставку
        if (driverCost.compareTo(BigDecimal.ZERO) == 0) {
            // Стандартная ставка: 1000 руб. за 8 часов + 5 руб/км
            BigDecimal standardHourlyRate = new BigDecimal("125"); // 1000 руб / 8 часов
            BigDecimal standardKmRate = new BigDecimal("5");

            BigDecimal hours = new BigDecimal(route.getDuration())
                    .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);

            driverCost = hours.multiply(standardHourlyRate)
                    .add(route.getDistance().multiply(standardKmRate));
        }

        route.setEstimatedDriverCost(driverCost.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Рассчитывает общую стоимость поездки.
     *
     * @param route построенный маршрут
     */
    private void calculateTotalCost(RouteResponseDto route) {
        BigDecimal totalCost = BigDecimal.ZERO;

        // Суммируем все виды затрат
        if (route.getEstimatedFuelCost() != null) {
            totalCost = totalCost.add(route.getEstimatedFuelCost());
        }

        if (route.getEstimatedTollCost() != null) {
            totalCost = totalCost.add(route.getEstimatedTollCost());
        }

        if (route.getEstimatedDriverCost() != null) {
            totalCost = totalCost.add(route.getEstimatedDriverCost());
        }

        // В реальном приложении здесь также будут учитываться другие виды затрат:
        // - износ ТС
        // - страховка
        // - техническое обслуживание
        // - амортизация и т.д.

        route.setEstimatedTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Обновляет данные о фактических затратах на основе данных телематики.
     *
     * @param route сущность маршрута, для которой обновляются данные
     * @param actualFuelConsumption фактический расход топлива
     * @param actualTime фактическое время в пути
     * @return обновленная сущность маршрута
     */
    public Route updateActualCosts(Route route, BigDecimal actualFuelConsumption, long actualTime) {
        if (route == null || actualFuelConsumption == null) {
            return route;
        }

        // Фактический расход топлива
        route.setActualFuelConsumption(actualFuelConsumption);

        // Фактическая стоимость топлива
        BigDecimal fuelPrice = DEFAULT_FUEL_PRICE; // В реальном приложении будет использоваться актуальная цена
        BigDecimal actualFuelCost = actualFuelConsumption.multiply(fuelPrice);

        // Обновляем фактическую общую стоимость
        // В реальном приложении здесь будут учитываться все компоненты фактических затрат
        BigDecimal actualTotalCost = actualFuelCost;
        if (route.getEstimatedTollCost() != null) {
            actualTotalCost = actualTotalCost.add(route.getEstimatedTollCost());
        }

        route.setActualTotalCost(actualTotalCost.setScale(2, RoundingMode.HALF_UP));

        return route;
    }
}
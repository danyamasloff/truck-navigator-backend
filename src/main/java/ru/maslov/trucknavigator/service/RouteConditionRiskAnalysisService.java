package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.dto.weather.*;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.integration.openweather.WeatherService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Сервис для анализа рисков маршрута.
 * Анализирует погодные условия, качество дорог и другие факторы,
 * влияющие на безопасность перевозки.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class    RouteConditionRiskAnalysisService {

    private final WeatherService weatherService;
    private final RouteWeatherService routeWeatherService;

    // Весовые коэффициенты для расчета общего риска, настраиваются в конфигурации
    @Value("${risk.analysis.weather.weight:0.4}")
    private double weatherRiskWeight;

    @Value("${risk.analysis.road.quality.weight:0.3}")
    private double roadQualityRiskWeight;

    @Value("${risk.analysis.accident.statistics.weight:0.3}")
    private double accidentStatisticsRiskWeight;

    /**
     * Анализирует риски для заданного маршрута с учетом транспортного средства и груза.
     *
     * @param route построенный маршрут
     * @param vehicle транспортное средство
     * @param cargo перевозимый груз (может быть null)
     * @return обновленный маршрут с добавленными оценками риска
     */
    public RouteResponseDto analyzeRouteRisks(RouteResponseDto route, Vehicle vehicle, Cargo cargo) {
        // Если маршрут не содержит координат, нечего анализировать
        if (route.getCoordinates() == null || route.getCoordinates().isEmpty()) {
            log.warn("Невозможно провести анализ рисков: маршрут не содержит координат");
            return route;
        }

        // Анализ рисков, связанных с погодой
        analyzeWeatherRisks(route);

        // Анализ рисков, связанных с качеством дорог
        analyzeRoadQualityRisks(route);

        // Расчет общего показателя риска
        calculateOverallRiskScore(route);

        // Добавление специфических рисков для типа груза
        if (cargo != null) {
            addCargoSpecificRisks(route, cargo);
        }

        // Добавление специфических рисков для транспортного средства
        addVehicleSpecificRisks(route, vehicle);

        return route;
    }

    /**
     * Анализирует погодные риски на протяжении маршрута.
     *
     * @param route построенный маршрут
     */
    private void analyzeWeatherRisks(RouteResponseDto route) {
        List<RouteResponseDto.WeatherAlertSegment> weatherAlerts = new ArrayList<>();
        double totalWeatherRiskScore = 0;
        int segmentsCount = 0;

        // Проверка возможности использования расширенного прогноза погоды
        boolean useDetailedForecast = route.getDepartureTime() != null;
        RouteWeatherForecastDto weatherForecast = null;

        if (useDetailedForecast) {
            try {
                // Получаем полный прогноз погоды для маршрута с учетом времени движения
                weatherForecast = routeWeatherService.generateRouteWeatherForecast(
                        route, route.getDepartureTime());

                // Если прогноз получен успешно, используем его данные для более точного анализа
                if (weatherForecast != null && !weatherForecast.getPointForecasts().isEmpty()) {
                    processDetailedWeatherForecast(route, weatherForecast, weatherAlerts);

                    // Суммируем оценки рисков из всех сегментов для расчета среднего
                    totalWeatherRiskScore = weatherAlerts.stream()
                            .mapToDouble(segment -> segment.getRiskScore().doubleValue())
                            .sum();
                    segmentsCount = weatherAlerts.size();

                    // Добавляем предупреждения о критичных погодных условиях
                    addCriticalWeatherWarnings(route, weatherForecast);

                    // После использования детального прогноза завершаем анализ
                    setWeatherRiskScore(route, weatherAlerts, totalWeatherRiskScore, segmentsCount);
                    return;
                }
            } catch (Exception e) {
                log.warn("Не удалось выполнить детальный анализ погодных рисков: {}", e.getMessage());
                // Если детальный анализ не удался, продолжаем с базовым подходом
            }
        }

        // Базовый анализ погоды (существующий код)
        List<double[]> coordinates = route.getCoordinates();
        int numSegments = Math.min(5, coordinates.size() / 10);
        numSegments = Math.max(1, numSegments);

        int pointsPerSegment = coordinates.size() / numSegments;

        for (int i = 0; i < numSegments; i++) {
            int startIndex = i * pointsPerSegment;
            int endIndex = (i == numSegments - 1) ? coordinates.size() - 1 : (i + 1) * pointsPerSegment - 1;

            // Получаем координаты середины сегмента для запроса погоды
            int midPointIndex = startIndex + (endIndex - startIndex) / 2;
            if (midPointIndex < coordinates.size()) {
                double[] midPoint = coordinates.get(midPointIndex);
                double lat = midPoint[1]; // Порядок в массиве [lon, lat]
                double lon = midPoint[0];

                try {
                    // Получаем текущую погоду для координат
                    WeatherDataDto weatherData = weatherService.getCurrentWeather(lat, lon);

                    // Оцениваем риск
                    String riskLevel = weatherData.calculateRiskLevel();
                    String riskDescription = weatherData.generateRiskDescription();
                    Integer riskScore = weatherData.getRiskScore();

                    if (riskScore > 0) {
                        // Добавляем сегмент с погодным предупреждением, если есть риск
                        RouteResponseDto.WeatherAlertSegment alert = new RouteResponseDto.WeatherAlertSegment();
                        alert.setStartIndex(startIndex);
                        alert.setEndIndex(endIndex);

                        // Рассчитываем расстояние сегмента
                        BigDecimal segmentDistance = calculateSegmentDistance(route.getDistance(), startIndex, endIndex, coordinates.size());
                        alert.setDistance(segmentDistance);

                        // Устанавливаем тип погодного явления
                        alert.setWeatherType(weatherData.getWeatherMain() != null ?
                                weatherData.getWeatherMain().toUpperCase() : "UNKNOWN");

                        alert.setSeverity(riskLevel);
                        alert.setDescription(riskDescription);
                        alert.setRiskScore(BigDecimal.valueOf(riskScore));

                        weatherAlerts.add(alert);

                        totalWeatherRiskScore += riskScore;
                        segmentsCount++;
                    }
                } catch (Exception e) {
                    log.error("Ошибка при получении данных о погоде для сегмента {}: {}", i, e.getMessage());
                }
            }
        }

        // Устанавливаем результат анализа
        setWeatherRiskScore(route, weatherAlerts, totalWeatherRiskScore, segmentsCount);
    }

    /**
     * Обрабатывает детальный прогноз погоды и создает сегменты с предупреждениями
     */
    private void processDetailedWeatherForecast(
            RouteResponseDto route,
            RouteWeatherForecastDto weatherForecast,
            List<RouteResponseDto.WeatherAlertSegment> weatherAlerts) {

        List<double[]> coordinates = route.getCoordinates();

        // Обработка прогнозов по точкам
        for (RoutePointWeatherDto pointForecast : weatherForecast.getPointForecasts()) {
            WeatherDataDto weatherData = pointForecast.getWeatherData();

            // Пропускаем точки без опасных погодных условий
            if (weatherData.getRiskScore() == null || weatherData.getRiskScore() <= 20) {
                continue;
            }

            // Определяем индексы сегмента (перед и после точки)
            int pointIndex = pointForecast.getPointIndex();
            int startIndex = Math.max(0, pointIndex - 10);
            int endIndex = Math.min(coordinates.size() - 1, pointIndex + 10);

            // Создаем погодный сегмент
            RouteResponseDto.WeatherAlertSegment alert = new RouteResponseDto.WeatherAlertSegment();
            alert.setStartIndex(startIndex);
            alert.setEndIndex(endIndex);

            // Устанавливаем дистанцию
            alert.setDistance(BigDecimal.valueOf(pointForecast.getDistanceFromStart()));

            // Устанавливаем тип погодного явления
            alert.setWeatherType(weatherData.getWeatherMain() != null ?
                    weatherData.getWeatherMain().toUpperCase() : "UNKNOWN");

            // Устанавливаем уровень опасности и описание
            alert.setSeverity(weatherData.getRiskLevel());
            alert.setDescription(weatherData.getRiskDescription() +
                    String.format(" (Прогноз на %s)",
                            pointForecast.getEstimatedTime().toLocalTime()));
            alert.setRiskScore(BigDecimal.valueOf(weatherData.getRiskScore()));

            weatherAlerts.add(alert);
        }
    }

    /**
     * Добавляет критические погодные предупреждения из прогноза в маршрут
     */
    private void addCriticalWeatherWarnings(RouteResponseDto route, RouteWeatherForecastDto weatherForecast) {
        if (weatherForecast.getHazardWarnings() == null || weatherForecast.getHazardWarnings().isEmpty()) {
            return;
        }

        // Фильтруем только критичные предупреждения (HIGH и SEVERE)
        List<WeatherHazardWarningDto> criticalWarnings = weatherForecast.getHazardWarnings()
                .stream()
                .filter(warning ->
                        warning.getSeverity() == HazardSeverity.HIGH ||
                                warning.getSeverity() == HazardSeverity.SEVERE)
                .toList();

        if (criticalWarnings.isEmpty()) {
            return;
        }

        // Добавляем информацию о критичных погодных условиях в список предупреждений маршрута
        List<String> routeWarnings = route.getRtoWarnings();
        if (routeWarnings == null) {
            routeWarnings = new ArrayList<>();
            route.setRtoWarnings(routeWarnings);
        }

        for (WeatherHazardWarningDto warning : criticalWarnings) {
            String formattedTime = warning.getExpectedTime().toLocalTime().toString();
            String warningText = String.format("[%s] %s - %s. %s",
                    formattedTime,
                    warning.getHazardType().toString(),
                    warning.getDescription(),
                    warning.getRecommendation());

            routeWarnings.add(warningText);
        }
    }

    /**
     * Устанавливает оценку погодного риска для маршрута
     */
    private void setWeatherRiskScore(
            RouteResponseDto route,
            List<RouteResponseDto.WeatherAlertSegment> weatherAlerts,
            double totalWeatherRiskScore,
            int segmentsCount) {

        // Устанавливаем результаты анализа в маршрут
        route.setWeatherAlertSegments(weatherAlerts);

        // Рассчитываем средний показатель риска по погоде
        if (segmentsCount > 0) {
            route.setWeatherRiskScore(BigDecimal.valueOf(totalWeatherRiskScore / segmentsCount)
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            route.setWeatherRiskScore(BigDecimal.ZERO);
        }
    }

    /**
     * Анализирует риски, связанные с качеством дорог на протяжении маршрута.
     *
     * @param route построенный маршрут
     */
    private void analyzeRoadQualityRisks(RouteResponseDto route) {
        List<RouteResponseDto.RoadQualitySegment> roadQualitySegments = new ArrayList<>();
        double totalRoadQualityRiskScore = 0;
        int segmentsCount = 0;

        // ЗАГЛУШКА: В реальном приложении здесь будет получение данных о качестве дорог из внешнего API или базы данных
        // Для примера генерируем случайные данные о качестве дорог

        // Разделяем маршрут на сегменты для анализа
        List<double[]> coordinates = route.getCoordinates();
        int numSegments = Math.min(8, coordinates.size() / 20); // Максимум 8 сегментов
        numSegments = Math.max(3, numSegments); // Минимум 3 сегмента

        int pointsPerSegment = coordinates.size() / numSegments;
        Random random = new Random();

        String[] qualities = {"EXCELLENT", "GOOD", "FAIR", "POOR", "VERY_POOR"};
        String[] surfaceTypes = {"ASPHALT", "CONCRETE", "GRAVEL", "UNPAVED"};

        // Для каждого сегмента генерируем данные о качестве дороги
        for (int i = 0; i < numSegments; i++) {
            int startIndex = i * pointsPerSegment;
            int endIndex = (i == numSegments - 1) ? coordinates.size() - 1 : (i + 1) * pointsPerSegment - 1;

            // Генерируем данные о качестве дорог (в реальном приложении здесь будет обращение к API)
            String quality = qualities[random.nextInt(qualities.length)];
            String surfaceType = surfaceTypes[random.nextInt(surfaceTypes.length)];

            // Рассчитываем риск в зависимости от качества
            int riskScore;
            switch (quality) {
                case "VERY_POOR":
                    riskScore = 70 + random.nextInt(30); // 70-100
                    break;
                case "POOR":
                    riskScore = 50 + random.nextInt(20); // 50-70
                    break;
                case "FAIR":
                    riskScore = 30 + random.nextInt(20); // 30-50
                    break;
                case "GOOD":
                    riskScore = 10 + random.nextInt(20); // 10-30
                    break;
                case "EXCELLENT":
                default:
                    riskScore = random.nextInt(10); // 0-10
                    break;
            }

            // Создаем сегмент с данными о качестве дороги
            RouteResponseDto.RoadQualitySegment segment = new RouteResponseDto.RoadQualitySegment();
            segment.setStartIndex(startIndex);
            segment.setEndIndex(endIndex);

            // Рассчитываем расстояние сегмента
            BigDecimal segmentDistance = calculateSegmentDistance(route.getDistance(), startIndex, endIndex, coordinates.size());
            segment.setDistance(segmentDistance);

            segment.setQuality(quality);
            segment.setSurfaceType(surfaceType);
            segment.setRiskScore(BigDecimal.valueOf(riskScore));

            // Генерируем описание в зависимости от качества дороги
            String description;
            switch (quality) {
                case "VERY_POOR":
                    description = "Дорога в очень плохом состоянии. Множественные выбоины, значительные повреждения покрытия.";
                    break;
                case "POOR":
                    description = "Дорога в плохом состоянии. Выбоины, трещины, необходим ремонт.";
                    break;
                case "FAIR":
                    description = "Дорога в удовлетворительном состоянии. Местами есть повреждения покрытия.";
                    break;
                case "GOOD":
                    description = "Дорога в хорошем состоянии. Незначительные дефекты покрытия.";
                    break;
                case "EXCELLENT":
                default:
                    description = "Дорога в отличном состоянии. Ровное покрытие без дефектов.";
                    break;
            }
            segment.setDescription(description);

            roadQualitySegments.add(segment);

            totalRoadQualityRiskScore += riskScore;
            segmentsCount++;
        }

        // Устанавливаем результаты анализа в маршрут
        route.setRoadQualitySegments(roadQualitySegments);

        // Рассчитываем средний показатель риска по качеству дорог
        if (segmentsCount > 0) {
            route.setRoadQualityRiskScore(BigDecimal.valueOf(totalRoadQualityRiskScore / segmentsCount)
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            route.setRoadQualityRiskScore(BigDecimal.ZERO);
        }
    }

    /**
     * Рассчитывает общий показатель риска на основе всех факторов.
     *
     * @param route построенный маршрут
     */
    private void calculateOverallRiskScore(RouteResponseDto route) {
        // Получаем оценки риска для разных факторов
        BigDecimal weatherRisk = route.getWeatherRiskScore() != null ? route.getWeatherRiskScore() : BigDecimal.ZERO;
        BigDecimal roadQualityRisk = route.getRoadQualityRiskScore() != null ? route.getRoadQualityRiskScore() : BigDecimal.ZERO;

        // В реальном приложении здесь будет оценка риска на основе статистики ДТП
        BigDecimal trafficRisk = BigDecimal.valueOf(new Random().nextInt(50)); // Заглушка
        route.setTrafficRiskScore(trafficRisk);

        // Рассчитываем взвешенную сумму рисков
        double overallRisk = weatherRisk.doubleValue() * weatherRiskWeight +
                roadQualityRisk.doubleValue() * roadQualityRiskWeight +
                trafficRisk.doubleValue() * accidentStatisticsRiskWeight;

        // Устанавливаем общий показатель риска
        route.setOverallRiskScore(BigDecimal.valueOf(overallRisk)
                .setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Добавляет специфические риски в зависимости от типа груза.
     *
     * @param route построенный маршрут
     * @param cargo перевозимый груз
     */
    private void addCargoSpecificRisks(RouteResponseDto route, Cargo cargo) {
        // Проверяем специфические риски для разных типов грузов

        // Хрупкий груз
        if (cargo.isFragile()) {
            // Увеличиваем риск для участков с плохим качеством дороги
            for (RouteResponseDto.RoadQualitySegment segment : route.getRoadQualitySegments()) {
                if ("POOR".equals(segment.getQuality()) || "VERY_POOR".equals(segment.getQuality())) {
                    // Увеличиваем оценку риска для хрупких грузов
                    double newRiskScore = segment.getRiskScore().doubleValue() * 1.5;
                    segment.setRiskScore(BigDecimal.valueOf(newRiskScore).setScale(2, RoundingMode.HALF_UP));

                    // Дополняем описание
                    segment.setDescription(segment.getDescription() + " Высокий риск повреждения хрупкого груза!");
                }
            }
        }

        // Груз с температурным режимом
        if (cargo.isRequiresTemperatureControl()) {
            int minTemp = cargo.getMinTemperatureCelsius();
            int maxTemp = cargo.getMaxTemperatureCelsius();

            // Проверяем соответствие температуры требованиям груза
            for (RouteResponseDto.WeatherAlertSegment segment : route.getWeatherAlertSegments()) {
                // Предполагаем, что у нас есть информация о температуре в сегменте (в реальном приложении)
                // Здесь просто добавляем предупреждение в описание
                segment.setDescription(segment.getDescription() +
                        " Требуется контроль температуры груза (диапазон " + minTemp + "°C до " + maxTemp + "°C).");
            }
        }

        // Опасный груз
        if (cargo.isDangerous()) {
            // Добавляем особые предупреждения и ограничения для опасных грузов
            String dangerousClassInfo = cargo.getDangerousGoodsClass() != null ?
                    (" класса " + cargo.getDangerousGoodsClass()) : "";

            // Увеличиваем общую оценку риска для опасных грузов
            double newOverallRisk = route.getOverallRiskScore().doubleValue() * 1.2;
            route.setOverallRiskScore(BigDecimal.valueOf(newOverallRisk).setScale(2, RoundingMode.HALF_UP));

            // Добавляем предупреждения о погодных рисках для опасных грузов
            for (RouteResponseDto.WeatherAlertSegment segment : route.getWeatherAlertSegments()) {
                if ("HIGH".equals(segment.getSeverity()) || "SEVERE".equals(segment.getSeverity())) {
                    segment.setDescription(segment.getDescription() +
                            " Повышенный риск при перевозке опасного груза" + dangerousClassInfo + "!");
                }
            }
        }
    }

    /**
     * Добавляет специфические риски в зависимости от характеристик транспортного средства.
     *
     * @param route построенный маршрут
     * @param vehicle транспортное средство
     */
    private void addVehicleSpecificRisks(RouteResponseDto route, Vehicle vehicle) {
        // Проверяем специфические риски для разных характеристик ТС

        // Высокое ТС (риск при сильном ветре)
        if (vehicle.getHeightCm() > 350) { // Высота более 3.5 метров
            for (RouteResponseDto.WeatherAlertSegment segment : route.getWeatherAlertSegments()) {
                if ("STRONG_WIND".equals(segment.getWeatherType()) || segment.getDescription().contains("ветер")) {
                    // Увеличиваем оценку риска для высоких ТС при сильном ветре
                    double newRiskScore = segment.getRiskScore().doubleValue() * 1.3;
                    segment.setRiskScore(BigDecimal.valueOf(newRiskScore).setScale(2, RoundingMode.HALF_UP));

                    // Дополняем описание
                    segment.setDescription(segment.getDescription() +
                            " Повышенный риск опрокидывания для высокого ТС!");
                }
            }
        }

        // Тяжелое ТС (риск на дорогах с плохим покрытием)
        if (vehicle.getGrossWeightKg() > 20000) { // Более 20 тонн
            for (RouteResponseDto.RoadQualitySegment segment : route.getRoadQualitySegments()) {
                if ("UNPAVED".equals(segment.getSurfaceType()) || "GRAVEL".equals(segment.getSurfaceType())) {
                    // Увеличиваем оценку риска для тяжелых ТС на грунтовых дорогах
                    double newRiskScore = segment.getRiskScore().doubleValue() * 1.4;
                    segment.setRiskScore(BigDecimal.valueOf(newRiskScore).setScale(2, RoundingMode.HALF_UP));

                    // Дополняем описание
                    segment.setDescription(segment.getDescription() +
                            " Высокий риск для тяжелого ТС на этом типе покрытия!");
                }
            }
        }
    }

    /**
     * Рассчитывает расстояние для сегмента маршрута.
     *
     * @param totalDistance общее расстояние маршрута
     * @param startIndex начальный индекс сегмента
     * @param endIndex конечный индекс сегмента
     * @param totalPoints общее количество точек маршрута
     * @return расстояние сегмента
     */
    private BigDecimal calculateSegmentDistance(BigDecimal totalDistance, int startIndex, int endIndex, int totalPoints) {
        // Предполагаем, что расстояние пропорционально количеству точек
        double segmentFraction = (double) (endIndex - startIndex + 1) / totalPoints;
        double segmentDistance = totalDistance.doubleValue() * segmentFraction;

        return BigDecimal.valueOf(segmentDistance).setScale(2, RoundingMode.HALF_UP);
    }
}
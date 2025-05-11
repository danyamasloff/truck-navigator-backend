package ru.maslov.trucknavigator.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.dto.weather.*;
import ru.maslov.trucknavigator.integration.openweather.WeatherService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteWeatherService {
    private final WeatherService weatherService;

    /**
     * Создает прогноз погоды для точек маршрута с учетом времени движения.
     * Результат кэшируется для избежания лишних запросов к API погоды.
     *
     * @param route маршрут для анализа
     * @param departureTime время отправления
     * @return объект с прогнозом погоды для маршрута
     */
    @Cacheable(value = "routeWeatherCache",
            key = "{#route.coordinates[0][0],#route.coordinates[0][1]," +
                    "#route.coordinates[#route.coordinates.size()-1][0]," +
                    "#route.coordinates[#route.coordinates.size()-1][1]," +
                    "#departureTime.toLocalDate()+" +
                    "#departureTime.getHour()}")
    public RouteWeatherForecastDto generateRouteWeatherForecast(
            RouteResponseDto route,
            LocalDateTime departureTime) {

        if (route == null || route.getCoordinates() == null || route.getCoordinates().isEmpty()) {
            log.warn("Невозможно создать прогноз погоды: некорректный маршрут");
            return null;
        }

        // Результирующий объект
        RouteWeatherForecastDto forecast = new RouteWeatherForecastDto();
        forecast.setDepartureTime(departureTime);
        forecast.setRoute(route);

        List<RoutePointWeatherDto> pointForecasts = new ArrayList<>();

        // Расчет времени для каждой значимой точки маршрута
        List<RouteTimePoint> timePoints = calculateRouteTimePoints(route, departureTime);

        // Получение прогноза для каждой значимой точки
        for (RouteTimePoint point : timePoints) {
            try {
                WeatherDataDto weatherData = weatherService.getForecastForTime(
                        point.getLat(),
                        point.getLon(),
                        point.getEstimatedTime());

                if (weatherData != null) {
                    // Анализ рисков для этой точки и времени
                    analyzeAndEnhanceWeatherData(weatherData, route);

                    RoutePointWeatherDto pointForecast = new RoutePointWeatherDto();
                    pointForecast.setPointIndex(point.getIndex());
                    pointForecast.setDistanceFromStart(point.getDistance());
                    pointForecast.setEstimatedTime(point.getEstimatedTime());
                    pointForecast.setWeatherData(weatherData);

                    pointForecasts.add(pointForecast);
                }
            } catch (Exception e) {
                log.error("Ошибка при получении прогноза погоды для точки {}: {}",
                        point.getIndex(), e.getMessage());
            }
        }

        forecast.setPointForecasts(pointForecasts);

        // Генерация предупреждений на основе погодных данных
        List<WeatherHazardWarningDto> hazardWarnings = generateHazardWarnings(pointForecasts, route);
        forecast.setHazardWarnings(hazardWarnings);

        return forecast;
    }

    /**
     * Рассчитывает временные точки на маршруте
     */
    private List<RouteTimePoint> calculateRouteTimePoints(
            RouteResponseDto route, LocalDateTime departureTime) {

        List<RouteTimePoint> timePoints = new ArrayList<>();

        // Точки интереса: начало, конец и каждые ~50 км или 30 минут пути
        // (или точки с резкими изменениями направления)
        List<double[]> coordinates = route.getCoordinates();
        int totalPoints = coordinates.size();

        // Общее время и дистанция
        long totalDurationMinutes = route.getDuration();
        double totalDistanceKm = route.getDistance().doubleValue();

        // Добавляем начальную точку
        timePoints.add(new RouteTimePoint(
                0, coordinates.get(0)[1], coordinates.get(0)[0],
                0.0, departureTime));

        // Расчет интервала для равномерного распределения точек
        // Примерно 10-15 точек на маршрут (не более)
        int interval = Math.max(totalPoints / 10, 1);

        for (int i = interval; i < totalPoints - 1; i += interval) {
            double[] coord = coordinates.get(i);

            // Расчет пройденной дистанции и времени
            double distanceFraction = (double) i / totalPoints;
            double distanceKm = totalDistanceKm * distanceFraction;

            // Расчет времени прибытия в точку
            long minutesPassed = (long) (totalDurationMinutes * distanceFraction);
            LocalDateTime pointTime = departureTime.plusMinutes(minutesPassed);

            timePoints.add(new RouteTimePoint(
                    i, coord[1], coord[0], distanceKm, pointTime));
        }

        // Добавляем конечную точку
        double[] endCoord = coordinates.get(totalPoints - 1);
        timePoints.add(new RouteTimePoint(
                totalPoints - 1, endCoord[1], endCoord[0],
                totalDistanceKm, departureTime.plusMinutes(totalDurationMinutes)));

        return timePoints;
    }

    /**
     * Анализирует погодные данные и добавляет информацию о рисках
     */
    private void analyzeAndEnhanceWeatherData(WeatherDataDto weatherData, RouteResponseDto route) {
        // Расчет рисков уже реализован в WeatherDataDto, вызываем методы
        weatherData.calculateRiskLevel();
        weatherData.generateRiskDescription();

        // Дополнительная логика анализа рисков может быть добавлена здесь
    }

    /**
     * Генерирует предупреждения о погодных опасностях на маршруте
     */
    private List<WeatherHazardWarningDto> generateHazardWarnings(
            List<RoutePointWeatherDto> pointForecasts, RouteResponseDto route) {

        List<WeatherHazardWarningDto> warnings = new ArrayList<>();

        // Анализируем прогноз для каждой точки и создаем предупреждения
        // по определенным критериям
        for (RoutePointWeatherDto pointForecast : pointForecasts) {
            WeatherDataDto weather = pointForecast.getWeatherData();

            // Проверка на сильный ветер - опасность опрокидывания
            if (weather.getWindSpeed() > 15.0) { // 15 м/с или более
                WeatherHazardWarningDto warning = new WeatherHazardWarningDto();
                warning.setHazardType(WeatherHazardType.STRONG_WIND);
                warning.setSeverity(calculateWindSeverity(weather.getWindSpeed()));
                warning.setDistanceFromStart(pointForecast.getDistanceFromStart());
                warning.setExpectedTime(pointForecast.getEstimatedTime());
                warning.setDescription(generateWindWarningText(weather));
                warning.setRecommendation("Снизьте скорость, держите руль крепче. " +
                        "Избегайте резких маневров.");
                warnings.add(warning);
            }

            // Предупреждение о возможной гололедице
            if (weather.getTemperature() < 3.0 &&
                    (weather.getRainVolume1h() != null || weather.getSnowVolume1h() != null)) {
                WeatherHazardWarningDto warning = new WeatherHazardWarningDto();
                warning.setHazardType(WeatherHazardType.ICE_RISK);
                warning.setSeverity(determineIceRiskSeverity(weather));
                warning.setDistanceFromStart(pointForecast.getDistanceFromStart());
                warning.setExpectedTime(pointForecast.getEstimatedTime());
                warning.setDescription("Возможная гололедица из-за низкой температуры и осадков. " +
                        String.format("Температура: %.1f°C", weather.getTemperature()));
                warning.setRecommendation("Снизьте скорость, увеличьте дистанцию, избегайте " +
                        "резких торможений. При необходимости используйте цепи противоскольжения.");
                warnings.add(warning);
            }

            // Прочие проверки (туман, сильный дождь, снегопад и т.д.)
            checkForLowVisibilityWarning(weather, pointForecast, warnings);
            checkForHeavyPrecipitation(weather, pointForecast, warnings);
        }

        return warnings;
    }

    // Вспомогательные методы для определения серьезности различных погодных условий

    private HazardSeverity calculateWindSeverity(double windSpeed) {
        if (windSpeed >= 25.0) return HazardSeverity.SEVERE;
        if (windSpeed >= 20.0) return HazardSeverity.HIGH;
        if (windSpeed >= 15.0) return HazardSeverity.MODERATE;
        return HazardSeverity.LOW;
    }

    private String generateWindWarningText(WeatherDataDto weather) {
        return String.format(
                "Сильный ветер %.1f м/с с направления %d°. " +
                        "Риск потери устойчивости транспортного средства.",
                weather.getWindSpeed(), weather.getWindDirection());
    }

    private HazardSeverity determineIceRiskSeverity(WeatherDataDto weather) {
        // Логика определения серьезности риска гололедицы
        if (weather.getTemperature() < -2.0 &&
                (weather.getRainVolume1h() != null && weather.getRainVolume1h() > 1.0)) {
            return HazardSeverity.SEVERE;
        }

        if (weather.getTemperature() < 0.0) {
            return HazardSeverity.HIGH;
        }

        return HazardSeverity.MODERATE;
    }

    private void checkForLowVisibilityWarning(
            WeatherDataDto weather,
            RoutePointWeatherDto pointForecast,
            List<WeatherHazardWarningDto> warnings) {

        // Проверка на туман или низкую видимость
        if (weather.getVisibility() != null && weather.getVisibility() < 1000) {
            WeatherHazardWarningDto warning = new WeatherHazardWarningDto();
            warning.setHazardType(WeatherHazardType.LOW_VISIBILITY);

            if (weather.getVisibility() < 200) {
                warning.setSeverity(HazardSeverity.SEVERE);
            } else if (weather.getVisibility() < 500) {
                warning.setSeverity(HazardSeverity.HIGH);
            } else {
                warning.setSeverity(HazardSeverity.MODERATE);
            }

            warning.setDistanceFromStart(pointForecast.getDistanceFromStart());
            warning.setExpectedTime(pointForecast.getEstimatedTime());
            warning.setDescription(String.format(
                    "Ограниченная видимость: %d метров. Возможен туман.",
                    weather.getVisibility()));
            warning.setRecommendation("Включите противотуманные фары, снизьте скорость, " +
                    "увеличьте дистанцию до других ТС.");

            warnings.add(warning);
        }
    }

    private void checkForHeavyPrecipitation(
            WeatherDataDto weather,
            RoutePointWeatherDto pointForecast,
            List<WeatherHazardWarningDto> warnings) {

        // Проверка на сильный дождь
        if (weather.getRainVolume1h() != null && weather.getRainVolume1h() > 4.0) {
            WeatherHazardWarningDto warning = new WeatherHazardWarningDto();
            warning.setHazardType(WeatherHazardType.HEAVY_RAIN);

            if (weather.getRainVolume1h() > 8.0) {
                warning.setSeverity(HazardSeverity.SEVERE);
            } else if (weather.getRainVolume1h() > 6.0) {
                warning.setSeverity(HazardSeverity.HIGH);
            } else {
                warning.setSeverity(HazardSeverity.MODERATE);
            }

            warning.setDistanceFromStart(pointForecast.getDistanceFromStart());
            warning.setExpectedTime(pointForecast.getEstimatedTime());
            warning.setDescription(String.format(
                    "Сильный дождь: %.1f мм/ч. Снижение сцепления с дорогой.",
                    weather.getRainVolume1h()));
            warning.setRecommendation("Снизьте скорость, увеличьте дистанцию, " +
                    "избегайте резких маневров.");

            warnings.add(warning);
        }

        // Проверка на снегопад
        if (weather.getSnowVolume1h() != null && weather.getSnowVolume1h() > 1.0) {
            WeatherHazardWarningDto warning = new WeatherHazardWarningDto();
            warning.setHazardType(WeatherHazardType.SNOW);

            if (weather.getSnowVolume1h() > 4.0) {
                warning.setSeverity(HazardSeverity.SEVERE);
            } else if (weather.getSnowVolume1h() > 2.0) {
                warning.setSeverity(HazardSeverity.HIGH);
            } else {
                warning.setSeverity(HazardSeverity.MODERATE);
            }

            warning.setDistanceFromStart(pointForecast.getDistanceFromStart());
            warning.setExpectedTime(pointForecast.getEstimatedTime());
            warning.setDescription(String.format(
                    "Снегопад: %.1f мм/ч. Ухудшение видимости и сцепления с дорогой.",
                    weather.getSnowVolume1h()));
            warning.setRecommendation("Снизьте скорость, увеличьте дистанцию, " +
                    "будьте готовы к использованию цепей противоскольжения.");

            warnings.add(warning);
        }
    }

    /**
     * Вспомогательный класс для хранения информации о точке маршрута с учетом времени
     */
    @Data
    @AllArgsConstructor
    private static class RouteTimePoint {
        private int index;        // Индекс точки в маршруте
        private double lat;       // Широта
        private double lon;       // Долгота
        private double distance;  // Расстояние от начала маршрута в км
        private LocalDateTime estimatedTime; // Предполагаемое время прибытия в точку
    }
}
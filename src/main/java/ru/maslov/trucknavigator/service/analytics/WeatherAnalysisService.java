package ru.maslov.trucknavigator.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.analytics.WeatherAnalysisDto;
import ru.maslov.trucknavigator.dto.analytics.WeatherPointDto;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.integration.openweather.WeatherService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для анализа погодных условий на маршруте.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherAnalysisService {

    private final WeatherService weatherService;

    /**
     * Анализирует погодные условия на протяжении маршрута.
     *
     * @param route маршрут для анализа
     * @return DTO с результатами анализа погоды
     */
    public WeatherAnalysisDto analyzeWeather(Route route) {
        if (route == null || route.getStartLat() == null || route.getStartLon() == null
                || route.getEndLat() == null || route.getEndLon() == null) {
            log.warn("Недостаточно данных для анализа погоды на маршруте");
            return createEmptyWeatherAnalysis();
        }

        log.debug("Анализ погодных условий для маршрута: {}", route.getName());

        List<WeatherPointDto> weatherPoints = new ArrayList<>();
        boolean hasPrecipitation = false;
        boolean hasStrongWind = false;
        boolean hasExtremeTemperature = false;
        double overallRiskScore = 0.0;

        try {
            // Анализ погоды в начальной точке
            WeatherPointDto startPoint = analyzeWeatherAtPoint(
                    route.getStartLat(), route.getStartLon(), route.getDepartureTime(), "START");
            weatherPoints.add(startPoint);

            // Анализ погоды в конечной точке
            LocalDateTime estimatedArrival = calculateEstimatedArrivalTime(route);
            WeatherPointDto endPoint = analyzeWeatherAtPoint(
                    route.getEndLat(), route.getEndLon(), estimatedArrival, "END");
            weatherPoints.add(endPoint);

            // Определение наличия осадков, сильного ветра или экстремальных температур
            for (WeatherPointDto point : weatherPoints) {
                if (point.isHasPrecipitation()) hasPrecipitation = true;
                if (point.getWindSpeed() > 8.0) hasStrongWind = true;
                if (point.getTemperature() < -15 || point.getTemperature() > 30) {
                    hasExtremeTemperature = true;
                }

                overallRiskScore += point.getWeatherRiskScore();
            }

            // Расчет среднего показателя риска
            overallRiskScore = overallRiskScore / weatherPoints.size();

        } catch (Exception e) {
            log.error("Ошибка при анализе погоды: {}", e.getMessage());
            return createEmptyWeatherAnalysis();
        }

        return WeatherAnalysisDto.builder()
                .weatherPoints(weatherPoints)
                .hasPrecipitation(hasPrecipitation)
                .hasStrongWind(hasStrongWind)
                .hasExtremeTemperature(hasExtremeTemperature)
                .overallWeatherRiskScore(BigDecimal.valueOf(overallRiskScore))
                .build();
    }

    /**
     * Анализирует погоду в конкретной точке маршрута.
     *
     * @param lat широта
     * @param lon долгота
     * @param time время
     * @param pointType тип точки (START, MIDDLE, END)
     * @return DTO с данными о погоде в точке
     */
    private WeatherPointDto analyzeWeatherAtPoint(double lat, double lon, LocalDateTime time, String pointType) {
        try {
            // Для примера заполняем данными, в реальном приложении здесь будет запрос к WeatherService
            double temperature = 15.0;
            double windSpeed = 5.0;
            String weatherCondition = "CLEAR";
            boolean hasPrecipitation = false;
            double precipitationAmount = 0.0;
            int weatherRiskScore = 10;

            return WeatherPointDto.builder()
                    .latitude(lat)
                    .longitude(lon)
                    .pointType(pointType)
                    .forecastTime(time)
                    .temperature(temperature)
                    .windSpeed(windSpeed)
                    .weatherCondition(weatherCondition)
                    .hasPrecipitation(hasPrecipitation)
                    .precipitationAmount(precipitationAmount)
                    .weatherRiskScore(weatherRiskScore)
                    .build();
        } catch (Exception e) {
            log.error("Ошибка при получении данных о погоде для точки {},{}: {}",
                    lat, lon, e.getMessage());
            return createEmptyWeatherPoint(lat, lon, time, pointType);
        }
    }

    /**
     * Создает пустой объект анализа погоды.
     *
     * @return пустой объект анализа погоды
     */
    private WeatherAnalysisDto createEmptyWeatherAnalysis() {
        return WeatherAnalysisDto.builder()
                .weatherPoints(new ArrayList<>())
                .hasPrecipitation(false)
                .hasStrongWind(false)
                .hasExtremeTemperature(false)
                .overallWeatherRiskScore(BigDecimal.ZERO)
                .build();
    }

    /**
     * Создает пустой объект с данными о погоде в точке.
     *
     * @param lat широта
     * @param lon долгота
     * @param time время
     * @param pointType тип точки
     * @return пустой объект с данными о погоде в точке
     */
    private WeatherPointDto createEmptyWeatherPoint(double lat, double lon, LocalDateTime time, String pointType) {
        return WeatherPointDto.builder()
                .latitude(lat)
                .longitude(lon)
                .pointType(pointType)
                .forecastTime(time)
                .temperature(0)
                .windSpeed(0)
                .weatherCondition("UNKNOWN")
                .hasPrecipitation(false)
                .precipitationAmount(0)
                .weatherRiskScore(0)
                .build();
    }

    /**
     * Рассчитывает предполагаемое время прибытия.
     *
     * @param route маршрут
     * @return предполагаемое время прибытия
     */
    private LocalDateTime calculateEstimatedArrivalTime(Route route) {
        if (route.getDepartureTime() != null && route.getEstimatedDurationMinutes() != null) {
            return route.getDepartureTime().plusMinutes(route.getEstimatedDurationMinutes());
        }
        // Если нет данных о времени отправления или длительности, используем текущее время + 1 час
        return LocalDateTime.now().plusHours(1);
    }
}

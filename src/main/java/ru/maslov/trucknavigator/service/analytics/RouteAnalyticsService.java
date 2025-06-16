package ru.maslov.trucknavigator.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.analytics.*;
import ru.maslov.trucknavigator.entity.*;

import java.util.List;

/**
 * Интегрированный сервис для комплексного анализа маршрутов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RouteAnalyticsService {

    private final RouteEconomicsService economicsService;
    private final DriverRestAnalysisService driverRestService;
    private final WeatherAnalysisService weatherAnalysisService;
    private final ComplianceRiskAnalysisService complianceRiskAnalysisService;

    /**
     * Выполняет комплексный анализ маршрута.
     *
     * @param route маршрут
     * @param vehicle транспортное средство
     * @param driver водитель
     * @param cargo груз
     * @param includeWeather включать ли анализ погоды
     * @return DTO с комплексным анализом
     */
    public RouteAnalyticsDto analyzeRoute(
            Route route, Vehicle vehicle, Driver driver, Cargo cargo, boolean includeWeather) {

        // Проверка входных данных
        if (route == null || vehicle == null) {
            throw new IllegalArgumentException("Маршрут и транспортное средство обязательны для анализа");
        }

        log.info("Начало комплексного анализа маршрута: {} ({} км)",
                route.getName(), route.getDistanceKm());

        // Экономический анализ
        RouteEconomicsDto economics = economicsService.analyzeRouteEconomics(route, vehicle, driver, cargo);
        log.debug("Завершен экономический анализ. Общая стоимость: {}", economics.getTotalCost());

        // Анализ РТО для водителя
        DriverRestAnalysisDto driverRest = null;
        if (driver != null) {
            driverRest = driverRestService.analyzeDriverRestTime(driver, route);
            log.debug("Завершен анализ РТО. Соответствие нормативам: {}", driverRest.getIsCompliant());
        }

        // Анализ погоды на маршруте
        WeatherAnalysisDto weatherAnalysis = null;
        if (includeWeather) {
            try {
                weatherAnalysis = weatherAnalysisService.analyzeWeather(route);
                log.debug("Завершен анализ погоды. Общая оценка риска: {}",
                        weatherAnalysis.getOverallWeatherRiskScore());
            } catch (Exception e) {
                log.warn("Не удалось выполнить анализ погоды: {}", e.getMessage());
            }
        }

        // Анализ рисков
        List<RiskFactorDto> riskFactors = complianceRiskAnalysisService.analyzeRouteRisks(route, vehicle, driver, cargo);
        log.debug("Завершен анализ рисков. Найдено {} факторов риска", riskFactors.size());

        // Интегральная оценка соответствия нормативам
        boolean isCompliant = (driverRest == null || driverRest.getIsCompliant());

        // Формируем итоговый анализ
        RouteAnalyticsDto analysisDto = RouteAnalyticsDto.builder()
                .economics(economics)
                .fuelConsumption(null) // Информация о расходе топлива уже включена в экономический анализ
                .driverRest(driverRest)
                .riskFactors(riskFactors)
                .weatherAnalysis(weatherAnalysis)
                .isCompliant(isCompliant)
                .build();

        // Формируем текстовое резюме анализа
        StringBuilder summary = new StringBuilder();
        summary.append("Маршрут: ").append(route.getName()).append("\n");
        summary.append("Расстояние: ").append(route.getDistanceKm()).append(" км\n");
        summary.append("Время в пути: ").append(formatDuration(route.getEstimatedDurationMinutes())).append("\n");
        summary.append("Общая стоимость: ").append(economics.getTotalCost())
                .append(" ").append(economics.getCurrency()).append("\n");

        if (driverRest != null) {
            summary.append("Режим труда и отдыха: ")
                    .append(driverRest.getIsCompliant() ? "соответствует нормативам" : "требует корректировки")
                    .append("\n");
            if (!driverRest.getRequiredRestStops().isEmpty()) {
                summary.append("Требуется ").append(driverRest.getRequiredRestStops().size())
                        .append(" остановок для отдыха\n");
            }
        }

        if (!riskFactors.isEmpty()) {
            summary.append("Выявлено ").append(riskFactors.size()).append(" факторов риска\n");
        }

        if (weatherAnalysis != null && weatherAnalysis.isHasPrecipitation()) {
            summary.append("На маршруте ожидаются осадки. ");
        }

        if (weatherAnalysis != null && weatherAnalysis.isHasStrongWind()) {
            summary.append("На маршруте ожидается сильный ветер. ");
        }

        summary.append("\nРекомендации учтены в анализе.");

        analysisDto.setSummary(summary.toString());

        log.info("Завершен комплексный анализ маршрута: {}", route.getName());
        return analysisDto;
    }

    /**
     * Форматирует продолжительность в минутах в удобочитаемый формат.
     *
     * @param minutes продолжительность в минутах
     * @return форматированная строка
     */
    private String formatDuration(Integer minutes) {
        if (minutes == null) {
            return "неизвестно";
        }

        int hours = minutes / 60;
        int mins = minutes % 60;

        if (hours > 0) {
            return String.format("%d ч %d мин", hours, mins);
        } else {
            return String.format("%d мин", mins);
        }
    }
}

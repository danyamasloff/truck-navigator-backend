package ru.maslov.trucknavigator.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.analytics.DriverRestAnalysisDto;
import ru.maslov.trucknavigator.dto.analytics.RestStopDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для анализа режима труда и отдыха водителей.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverRestAnalysisService {

    // Параметры РТО из конфигурации
    @Value("${driver.rest.short.break.minutes:15}")
    private int shortBreakMinutes;

    @Value("${driver.rest.long.break.minutes:45}")
    private int longBreakMinutes;

    @Value("${driver.work.max.continuous.minutes:270}")
    private int maxContinuousDrivingMinutes;

    @Value("${driver.work.max.daily.minutes:540}")
    private int maxDailyDrivingMinutes;

    /**
     * Анализирует режим труда и отдыха водителя для указанного маршрута.
     *
     * @param driver водитель
     * @param route маршрут
     * @return DTO с результатами анализа
     */
    public DriverRestAnalysisDto analyzeDriverRestTime(Driver driver, Route route) {
        if (driver == null || route == null || route.getEstimatedDurationMinutes() == null) {
            return DriverRestAnalysisDto.builder()
                    .isCompliant(false)
                    .complianceNotes("Недостаточно данных для анализа РТО")
                    .build();
        }

        // Текущее состояние водителя
        Integer continuousDrivingMinutes = driver.getContinuousDrivingMinutes();
        Integer dailyDrivingMinutesToday = driver.getDailyDrivingMinutesToday();

        // Если данные не указаны, используем значения по умолчанию
        if (continuousDrivingMinutes == null) {
            continuousDrivingMinutes = 0;
        }

        if (dailyDrivingMinutesToday == null) {
            dailyDrivingMinutesToday = 0;
        }

        // Оставшееся время вождения
        int remainingContinuousDrivingMinutes = maxContinuousDrivingMinutes - continuousDrivingMinutes;
        int remainingDailyDrivingMinutes = maxDailyDrivingMinutes - dailyDrivingMinutesToday;

        // Время в пути для маршрута
        int routeDurationMinutes = route.getEstimatedDurationMinutes();

        // Определяем, соответствует ли маршрут нормативам РТО
        boolean needsBreaks = routeDurationMinutes > remainingContinuousDrivingMinutes;
        boolean exceedsDailyLimit = routeDurationMinutes > remainingDailyDrivingMinutes;
        boolean isCompliant = !exceedsDailyLimit && (!needsBreaks || routeDurationMinutes <= remainingDailyDrivingMinutes);

        // Формируем DTO
        DriverRestAnalysisDto analysisDto = DriverRestAnalysisDto.builder()
                .driverId(driver.getId())
                .driverName(driver.getLastName() + " " + driver.getFirstName())
                .remainingContinuousDrivingMinutes(remainingContinuousDrivingMinutes)
                .remainingDailyDrivingMinutes(remainingDailyDrivingMinutes)
                .estimatedTripDurationMinutes(routeDurationMinutes)
                .isCompliant(isCompliant)
                .build();

        // Если маршрут требует перерывов, рассчитываем рекомендуемые остановки
        if (needsBreaks) {
            List<RestStopDto> restStops = calculateRequiredRestStops(
                    route, remainingContinuousDrivingMinutes, remainingDailyDrivingMinutes);
            analysisDto.setRequiredRestStops(restStops);
        }

        // Формируем пояснения о соответствии нормативам
        StringBuilder complianceNotes = new StringBuilder();

        if (isCompliant && !needsBreaks) {
            complianceNotes.append("Маршрут соответствует нормативам РТО без необходимости дополнительных остановок.");
        } else if (isCompliant) {
            complianceNotes.append("Маршрут соответствует нормативам РТО при условии соблюдения рекомендованных остановок для отдыха.");
        } else if (exceedsDailyLimit) {
            complianceNotes.append("Маршрут превышает допустимое время суточного вождения на ")
                    .append(routeDurationMinutes - remainingDailyDrivingMinutes)
                    .append(" минут. Рекомендуется разделить маршрут на два дня или назначить другого водителя.");
        } else {
            complianceNotes.append("Маршрут требует обязательных остановок для отдыха согласно нормативам РТО.");
        }

        analysisDto.setComplianceNotes(complianceNotes.toString());

        return analysisDto;
    }

    /**
     * Рассчитывает необходимые остановки для отдыха.
     *
     * @param route маршрут
     * @param remainingContinuousDrivingMinutes оставшееся время непрерывного вождения
     * @param remainingDailyDrivingMinutes оставшееся время суточного вождения
     * @return список рекомендуемых остановок
     */
    private List<RestStopDto> calculateRequiredRestStops(
            Route route, int remainingContinuousDrivingMinutes, int remainingDailyDrivingMinutes) {

        List<RestStopDto> restStops = new ArrayList<>();

        if (route.getDistanceKm() == null || route.getEstimatedDurationMinutes() == null
                || route.getDepartureTime() == null) {
            return restStops;
        }

        // Оставшееся время вождения
        int remainingContinuous = remainingContinuousDrivingMinutes;
        int remainingDaily = remainingDailyDrivingMinutes;

        // Общее время в пути
        int totalDurationMinutes = route.getEstimatedDurationMinutes();

        // Общее расстояние
        BigDecimal totalDistance = route.getDistanceKm();

        // Время отправления
        LocalDateTime departureTime = route.getDepartureTime();

        // Количество пройденных минут
        int elapsedMinutes = 0;

        while (elapsedMinutes < totalDurationMinutes) {
            // Определяем, какое ограничение сработает раньше
            int nextDrivingSegment;
            int restDuration;
            String description;

            if (remainingContinuous <= remainingDaily) {
                // Ограничение по непрерывному вождению
                nextDrivingSegment = Math.min(remainingContinuous, totalDurationMinutes - elapsedMinutes);
                restDuration = longBreakMinutes;
                description = "Обязательный отдых после непрерывного вождения " + maxContinuousDrivingMinutes + " минут";

                // Сбрасываем счетчик непрерывного вождения после отдыха
                remainingContinuous = maxContinuousDrivingMinutes;
            } else {
                // Ограничение по суточному вождению
                nextDrivingSegment = Math.min(remainingDaily, totalDurationMinutes - elapsedMinutes);

                if (nextDrivingSegment >= remainingContinuous) {
                    // Но сначала нужен перерыв по непрерывному вождению
                    nextDrivingSegment = remainingContinuous;
                    restDuration = longBreakMinutes;
                    description = "Обязательный отдых после непрерывного вождения " + maxContinuousDrivingMinutes + " минут";

                    // Сбрасываем счетчик непрерывного вождения после отдыха
                    remainingContinuous = maxContinuousDrivingMinutes;
                } else {
                    // Достигнут суточный лимит
                    restDuration = 660; // 11 часов (минимальный суточный отдых)
                    description = "Ежедневный отдых (достигнут суточный лимит вождения)";

                    // Сбрасываем оба счетчика после суточного отдыха
                    remainingContinuous = maxContinuousDrivingMinutes;
                    remainingDaily = maxDailyDrivingMinutes;
                }
            }

            // Перемещаемся вперед по маршруту
            elapsedMinutes += nextDrivingSegment;

            // Время прибытия в точку остановки
            LocalDateTime arrivalTime = departureTime.plusMinutes(elapsedMinutes);

            // Расстояние от начала маршрута до точки остановки
            BigDecimal distanceFromStart = totalDistance
                    .multiply(BigDecimal.valueOf(elapsedMinutes))
                    .divide(BigDecimal.valueOf(totalDurationMinutes), 2, BigDecimal.ROUND_HALF_UP);

            // Уменьшаем оставшееся время вождения
            remainingContinuous -= nextDrivingSegment;
            remainingDaily -= nextDrivingSegment;

            // Добавляем остановку, если еще не достигли конца маршрута
            if (elapsedMinutes < totalDurationMinutes) {
                RestStopDto restStop = RestStopDto.builder()
                        .minutesFromStart((long) elapsedMinutes)
                        .distanceFromStart(distanceFromStart)
                        .description(description)
                        .durationMinutes(restDuration)
                        .estimatedArrivalTime(arrivalTime)
                        .poiType("REST") // Тип точки интереса (можно определять динамически)
                        .build();

                restStops.add(restStop);

                // Учитываем время отдыха
                departureTime = arrivalTime.plusMinutes(restDuration);
            }
        }

        return restStops;
    }
}

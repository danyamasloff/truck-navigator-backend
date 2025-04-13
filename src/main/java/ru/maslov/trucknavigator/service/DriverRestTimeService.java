package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.driver.DriverRestAnalysisDto;
import ru.maslov.trucknavigator.dto.driver.RestStopRecommendationDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для контроля режима труда и отдыха водителя согласно нормативам.
 * Реализует логику проверки соответствия РТО законодательству РФ (Приказ №424)
 * и формирует рекомендации по планированию остановок для отдыха.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverRestTimeService {

    // Нормативы РТО из конфигурации
    @Value("${driver.rest.short.break.minutes:15}")
    private int shortBreakMinutes;

    @Value("${driver.rest.long.break.minutes:45}")
    private int longBreakMinutes;

    @Value("${driver.work.max.continuous.minutes:270}")
    private int maxContinuousDrivingMinutes;

    @Value("${driver.work.max.daily.minutes:540}")
    private int maxDailyDrivingMinutes;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Анализирует соответствие режима труда и отдыха водителя нормативам
     * на основе планируемого маршрута.
     *
     * @param driver информация о водителе
     * @param route маршрут
     * @param departureTime время отправления
     * @return результат анализа РТО с рекомендациями
     */
    public DriverRestAnalysisDto analyzeDriverRestTime(Driver driver, RouteResponseDto route, LocalDateTime departureTime) {
        if (driver == null || route == null || departureTime == null) {
            log.warn("Невозможно выполнить анализ РТО: недостаточно данных");
            return null;
        }

        DriverRestAnalysisDto analysis = new DriverRestAnalysisDto();
        analysis.setDriverId(driver.getId());
        analysis.setDriverName(driver.getLastName() + " " + driver.getFirstName());
        analysis.setDepartureTime(departureTime);

        // Расчет времени в пути и прибытия
        long durationInMinutes = route.getDuration();
        LocalDateTime arrivalTime = departureTime.plusMinutes(durationInMinutes);
        analysis.setEstimatedArrivalTime(arrivalTime);
        analysis.setEstimatedTripDurationMinutes((int) durationInMinutes);

        // Проверка текущего статуса вождения
        analysis.setCurrentDrivingStatus(driver.getCurrentDrivingStatus());
        analysis.setCurrentStatusStartTime(driver.getCurrentStatusStartTime());

        // Расчет остаточного времени вождения
        int remainingContinuousDrivingMinutes = calculateRemainingContinuousDrivingTime(driver);
        int remainingDailyDrivingMinutes = calculateRemainingDailyDrivingTime(driver);
        analysis.setRemainingContinuousDrivingMinutes(remainingContinuousDrivingMinutes);
        analysis.setRemainingDailyDrivingMinutes(remainingDailyDrivingMinutes);

        // Генерация рекомендаций по остановкам для отдыха
        List<RestStopRecommendationDto> recommendations = generateRestStopRecommendations(
                driver, route, departureTime, remainingContinuousDrivingMinutes, remainingDailyDrivingMinutes);
        analysis.setRestStopRecommendations(recommendations);

        // Проверка соответствия нормативам
        boolean compliant = checkComplianceWithRegulations(durationInMinutes,
                remainingContinuousDrivingMinutes, remainingDailyDrivingMinutes,
                recommendations.isEmpty());
        analysis.setCompliantWithRegulations(compliant);

        // Создание сводной информации
        analysis.setSummary(generateSummary(analysis));

        return analysis;
    }

    /**
     * Рассчитывает оставшееся время непрерывного вождения для водителя.
     *
     * @param driver информация о водителе
     * @return оставшееся время в минутах
     */
    private int calculateRemainingContinuousDrivingTime(Driver driver) {
        // Если водитель в режиме вождения, вычитаем уже проведенное время
        if (Driver.DrivingStatus.DRIVING.equals(driver.getCurrentDrivingStatus())
                && driver.getCurrentStatusStartTime() != null
                && driver.getContinuousDrivingMinutes() != null) {

            return maxContinuousDrivingMinutes - driver.getContinuousDrivingMinutes();
        }

        // Если водитель не за рулем или нет данных, возвращаем полное доступное время
        return maxContinuousDrivingMinutes;
    }

    /**
     * Рассчитывает оставшееся время суточного вождения для водителя.
     *
     * @param driver информация о водителе
     * @return оставшееся время в минутах
     */
    private int calculateRemainingDailyDrivingTime(Driver driver) {
        // Проверяем, сколько времени водитель уже отработал сегодня
        if (driver.getDailyDrivingMinutesToday() != null) {
            return maxDailyDrivingMinutes - driver.getDailyDrivingMinutesToday();
        }

        // Если нет данных, возвращаем полное доступное время
        return maxDailyDrivingMinutes;
    }

    /**
     * Генерирует рекомендации по остановкам для отдыха во время маршрута.
     *
     * @param driver информация о водителе
     * @param route маршрут
     * @param departureTime время отправления
     * @param remainingContinuousDrivingMinutes оставшееся время непрерывного вождения
     * @param remainingDailyDrivingMinutes оставшееся время суточного вождения
     * @return список рекомендаций по остановкам
     */
    private List<RestStopRecommendationDto> generateRestStopRecommendations(
            Driver driver, RouteResponseDto route, LocalDateTime departureTime,
            int remainingContinuousDrivingMinutes, int remainingDailyDrivingMinutes) {

        List<RestStopRecommendationDto> recommendations = new ArrayList<>();
        long totalDurationMinutes = route.getDuration();

        // Если общее время поездки меньше оставшегося времени непрерывного вождения,
        // остановки не требуются
        if (totalDurationMinutes <= remainingContinuousDrivingMinutes &&
                totalDurationMinutes <= remainingDailyDrivingMinutes) {
            return recommendations;
        }

        // Текущее время в пути
        int elapsedMinutes = 0;

        // Оставшееся время вождения
        int remainingContinuous = remainingContinuousDrivingMinutes;
        int remainingDaily = remainingDailyDrivingMinutes;

        // Текущее время (для расчета времени остановок)
        LocalDateTime currentTime = departureTime;

        // Формируем рекомендации, пока не дойдем до конца маршрута
        while (elapsedMinutes < totalDurationMinutes) {
            int nextDrivingSegment;
            int restDuration;
            String restType;
            String reason;

            // Определяем, какое ограничение сработает раньше
            if (remainingContinuous <= remainingDaily) {
                // Ограничение по непрерывному вождению
                nextDrivingSegment = Math.min(remainingContinuous, (int)(totalDurationMinutes - elapsedMinutes));
                restDuration = longBreakMinutes;
                restType = "Длительный отдых";
                reason = "Достигнут лимит непрерывного вождения";

                // После длительного отдыха сбрасываем счетчик непрерывного вождения
                remainingContinuous = maxContinuousDrivingMinutes;
            } else {
                // Ограничение по суточному вождению
                nextDrivingSegment = Math.min(remainingDaily, (int)(totalDurationMinutes - elapsedMinutes));

                if (nextDrivingSegment >= remainingContinuous) {
                    // Но сначала нужен перерыв по непрерывному вождению
                    nextDrivingSegment = remainingContinuous;
                    restDuration = longBreakMinutes;
                    restType = "Длительный отдых";
                    reason = "Достигнут лимит непрерывного вождения";

                    // После отдыха сбрасываем счетчик непрерывного вождения
                    remainingContinuous = maxContinuousDrivingMinutes;
                } else {
                    // Достигнут суточный лимит
                    restDuration = 660; // 11 часов (минимальный суточный отдых)
                    restType = "Суточный отдых";
                    reason = "Достигнут суточный лимит вождения";

                    // После суточного отдыха сбрасываем оба счетчика
                    remainingContinuous = maxContinuousDrivingMinutes;
                    remainingDaily = maxDailyDrivingMinutes;
                }
            }

            // Перемещаемся вперед по маршруту
            elapsedMinutes += nextDrivingSegment;
            currentTime = currentTime.plusMinutes(nextDrivingSegment);

            // Уменьшаем оставшееся время вождения
            remainingContinuous -= nextDrivingSegment;
            remainingDaily -= nextDrivingSegment;

            // Если мы еще не достигли конца маршрута, добавляем рекомендацию по остановке
            if (elapsedMinutes < totalDurationMinutes) {
                RestStopRecommendationDto recommendation = new RestStopRecommendationDto();
                recommendation.setDistanceFromStartKm(calculateDistanceAtTime(route, elapsedMinutes));
                recommendation.setTimeFromDeparture(formatDuration(Duration.ofMinutes(elapsedMinutes)));
                recommendation.setExpectedArrivalAtStop(currentTime);
                recommendation.setRecommendedRestDurationMinutes(restDuration);
                recommendation.setRestType(restType);
                recommendation.setReason(reason);

                // Добавляем координаты точки остановки (если возможно)
                if (route.getCoordinates() != null && !route.getCoordinates().isEmpty()) {
                    double[] coords = findCoordinatesAtDistance(route, recommendation.getDistanceFromStartKm());
                    if (coords != null) {
                        recommendation.setLongitude(coords[0]);
                        recommendation.setLatitude(coords[1]);
                    }
                }

                recommendations.add(recommendation);

                // Учитываем время отдыха
                currentTime = currentTime.plusMinutes(restDuration);
            }
        }

        return recommendations;
    }

    /**
     * Рассчитывает пройденное расстояние на указанной временной отметке маршрута.
     *
     * @param route маршрут
     * @param elapsedMinutes прошедшее время в минутах
     * @return пройденное расстояние в километрах
     */
    private double calculateDistanceAtTime(RouteResponseDto route, int elapsedMinutes) {
        // Предполагаем, что скорость постоянна на всем маршруте
        double fractionOfTotalTime = (double) elapsedMinutes / route.getDuration();
        return route.getDistance().doubleValue() * fractionOfTotalTime;
    }

    /**
     * Находит координаты точки на маршруте, соответствующей указанному расстоянию от начала.
     *
     * @param route маршрут
     * @param distanceKm расстояние от начала в километрах
     * @return координаты точки [долгота, широта] или null, если невозможно определить
     */
    private double[] findCoordinatesAtDistance(RouteResponseDto route, double distanceKm) {
        // Если нет координат, вернуть null
        if (route.getCoordinates() == null || route.getCoordinates().isEmpty()) {
            return null;
        }

        // Расчет индекса точки на основе пропорции
        double fractionOfTotalDistance = distanceKm / route.getDistance().doubleValue();
        int index = (int) (fractionOfTotalDistance * (route.getCoordinates().size() - 1));

        // Проверка на выход за границы массива
        if (index < 0) {
            index = 0;
        } else if (index >= route.getCoordinates().size()) {
            index = route.getCoordinates().size() - 1;
        }

        return route.getCoordinates().get(index);
    }

    /**
     * Проверяет соответствие маршрута нормативам РТО.
     *
     * @param totalDurationMinutes общая длительность маршрута в минутах
     * @param remainingContinuousDrivingMinutes оставшееся время непрерывного вождения
     * @param remainingDailyDrivingMinutes оставшееся время суточного вождения
     * @param hasNoRecommendations флаг отсутствия рекомендаций по остановкам
     * @return true, если маршрут соответствует нормативам
     */
    private boolean checkComplianceWithRegulations(long totalDurationMinutes,
                                                   int remainingContinuousDrivingMinutes,
                                                   int remainingDailyDrivingMinutes,
                                                   boolean hasNoRecommendations) {
        // Если нет рекомендаций по остановкам, значит маршрут соответствует нормативам
        if (hasNoRecommendations) {
            return true;
        }

        // Проверяем, есть ли превышение лимитов без учета остановок
        return !(totalDurationMinutes > remainingContinuousDrivingMinutes &&
                totalDurationMinutes > remainingDailyDrivingMinutes);
    }

    /**
     * Формирует текстовое резюме анализа РТО.
     *
     * @param analysis результат анализа РТО
     * @return текстовое описание
     */
    private String generateSummary(DriverRestAnalysisDto analysis) {
        StringBuilder summary = new StringBuilder();

        // Базовая информация
        summary.append(String.format("Водитель: %s\n", analysis.getDriverName()));
        summary.append(String.format("Отправление: %s\n",
                analysis.getDepartureTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        summary.append(String.format("Прибытие: %s\n",
                analysis.getEstimatedArrivalTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        summary.append(String.format("Время в пути: %s\n",
                formatDuration(Duration.ofMinutes(analysis.getEstimatedTripDurationMinutes()))));

        // Текущий статус и оставшееся время
        summary.append(String.format("Текущий статус: %s\n",
                formatDrivingStatus(analysis.getCurrentDrivingStatus())));

        summary.append(String.format("Оставшееся время непрерывного вождения: %d мин.\n",
                analysis.getRemainingContinuousDrivingMinutes()));
        summary.append(String.format("Оставшееся время суточного вождения: %d мин.\n",
                analysis.getRemainingDailyDrivingMinutes()));

        // Соответствие нормативам
        if (analysis.isCompliantWithRegulations()) {
            if (analysis.getRestStopRecommendations() == null || analysis.getRestStopRecommendations().isEmpty()) {
                summary.append("Маршрут соответствует нормативам РТО без необходимости остановок для отдыха.\n");
            } else {
                summary.append("Маршрут соответствует нормативам РТО при соблюдении рекомендованных остановок для отдыха.\n");
            }
        } else {
            summary.append("ВНИМАНИЕ! Маршрут не соответствует нормативам РТО. Требуется корректировка.\n");
        }

        // Рекомендации по остановкам
        if (analysis.getRestStopRecommendations() != null && !analysis.getRestStopRecommendations().isEmpty()) {
            summary.append("\nРекомендуемые остановки для отдыха:\n");

            for (int i = 0; i < analysis.getRestStopRecommendations().size(); i++) {
                RestStopRecommendationDto stop = analysis.getRestStopRecommendations().get(i);
                summary.append(String.format("%d. Через %s пути (%.1f км) - %s на %d мин. - %s\n",
                        i + 1,
                        stop.getTimeFromDeparture(),
                        stop.getDistanceFromStartKm(),
                        stop.getRestType(),
                        stop.getRecommendedRestDurationMinutes(),
                        stop.getReason()));
            }
        }

        return summary.toString();
    }

    /**
     * Форматирует продолжительность в удобочитаемый формат.
     *
     * @param duration продолжительность
     * @return строковое представление в формате "Xч Yмин"
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%dч %dмин", hours, minutes);
        } else {
            return String.format("%dмин", minutes);
        }
    }

    /**
     * Форматирует статус вождения в удобочитаемый формат.
     *
     * @param status статус вождения
     * @return строковое представление статуса
     */
    private String formatDrivingStatus(Driver.DrivingStatus status) {
        if (status == null) {
            return "Неизвестно";
        }

        switch (status) {
            case DRIVING:
                return "Вождение";
            case REST_BREAK:
                return "Короткий перерыв";
            case DAILY_REST:
                return "Ежедневный отдых";
            case WEEKLY_REST:
                return "Еженедельный отдых";
            case OTHER_WORK:
                return "Другая работа";
            case AVAILABILITY:
                return "В режиме готовности";
            default:
                return status.toString();
        }
    }

    /**
     * Обновляет статус вождения водителя.
     *
     * @param driver водитель
     * @param newStatus новый статус
     * @param statusChangeTime время изменения статуса
     * @return обновленный объект водителя
     */
    public Driver updateDriverStatus(Driver driver, Driver.DrivingStatus newStatus, LocalDateTime statusChangeTime) {
        if (driver == null || newStatus == null || statusChangeTime == null) {
            return driver;
        }

        // Сохраняем старый статус для расчетов
        Driver.DrivingStatus oldStatus = driver.getCurrentDrivingStatus();
        LocalDateTime oldStatusStartTime = driver.getCurrentStatusStartTime();

        // Если статус не меняется, просто возвращаем водителя
        if (newStatus == oldStatus) {
            return driver;
        }

        // Если был статус и время старта, обновляем счетчики
        if (oldStatus != null && oldStatusStartTime != null) {
            long minutesInPreviousStatus = Duration.between(oldStatusStartTime, statusChangeTime).toMinutes();

            // Обновляем счетчики в зависимости от старого статуса
            if (Driver.DrivingStatus.DRIVING.equals(oldStatus)) {
                // Увеличиваем счетчик непрерывного вождения
                int continuousDriving = driver.getContinuousDrivingMinutes() != null ?
                        driver.getContinuousDrivingMinutes() : 0;
                driver.setContinuousDrivingMinutes(continuousDriving + (int) minutesInPreviousStatus);

                // Увеличиваем счетчик суточного вождения
                int dailyDriving = driver.getDailyDrivingMinutesToday() != null ?
                        driver.getDailyDrivingMinutesToday() : 0;
                driver.setDailyDrivingMinutesToday(dailyDriving + (int) minutesInPreviousStatus);

                // Увеличиваем счетчик недельного вождения
                int weeklyDriving = driver.getWeeklyDrivingMinutes() != null ?
                        driver.getWeeklyDrivingMinutes() : 0;
                driver.setWeeklyDrivingMinutes(weeklyDriving + (int) minutesInPreviousStatus);
            }
        }

        // Устанавливаем новый статус и время
        driver.setCurrentDrivingStatus(newStatus);
        driver.setCurrentStatusStartTime(statusChangeTime);

        // Если новый статус - отдых, сбрасываем счетчик непрерывного вождения
        if (Driver.DrivingStatus.REST_BREAK.equals(newStatus) ||
                Driver.DrivingStatus.DAILY_REST.equals(newStatus) ||
                Driver.DrivingStatus.WEEKLY_REST.equals(newStatus)) {

            // Длительный отдых сбрасывает счетчик непрерывного вождения
            if (oldStatus != null && oldStatusStartTime != null) {
                long restMinutes = Duration.between(oldStatusStartTime, statusChangeTime).toMinutes();
                if (restMinutes >= longBreakMinutes) {
                    driver.setContinuousDrivingMinutes(0);
                }
            }

            // Суточный отдых сбрасывает счетчик суточного вождения
            if (Driver.DrivingStatus.DAILY_REST.equals(newStatus) ||
                    Driver.DrivingStatus.WEEKLY_REST.equals(newStatus)) {
                driver.setDailyDrivingMinutesToday(0);
            }

            // Недельный отдых сбрасывает счетчик недельного вождения
            if (Driver.DrivingStatus.WEEKLY_REST.equals(newStatus)) {
                driver.setWeeklyDrivingMinutes(0);
            }
        }

        return driver;
    }
}
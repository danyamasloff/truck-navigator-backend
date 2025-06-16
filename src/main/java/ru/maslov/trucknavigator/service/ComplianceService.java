package ru.maslov.trucknavigator.service;
import ru.maslov.trucknavigator.entity.DrivingStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.compliance.ComplianceResultDto;
import ru.maslov.trucknavigator.entity.Driver;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для контроля соответствия нормативным требованиям.
 * В частности, контролирует режим труда и отдыха (РТО) водителей.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceService {

    // Нормативы РТО из конфигурации
    @Value("${driver.rest.short.break.minutes:15}")
    private int shortBreakMinutes;

    @Value("${driver.rest.long.break.minutes:45}")
    private int longBreakMinutes;

    @Value("${driver.work.max.continuous.minutes:270}")
    private int maxContinuousDrivingMinutes;

    @Value("${driver.work.max.daily.minutes:540}")
    private int maxDailyDrivingMinutes;

    /**
     * Проверяет соответствие маршрута нормативам РТО для водителя.
     *
     * @param driver водитель
     * @param routeDurationMinutes продолжительность маршрута в минутах
     * @return результат проверки соответствия нормативам
     */
    public ComplianceResultDto checkRtoCompliance(Driver driver, long routeDurationMinutes) {
        ComplianceResultDto result = new ComplianceResultDto();
        List<String> warnings = new ArrayList<>();

        // Если нет данных о водителе, возвращаем результат без проверок
        if (driver == null) {
            result.setCompliant(true);
            return result;
        }

        // Текущее состояние водителя
        DrivingStatus currentStatus = driver.getCurrentDrivingStatus();
        LocalDateTime statusStartTime = driver.getCurrentStatusStartTime();
        Integer continuousDrivingMinutes = driver.getContinuousDrivingMinutes();
        Integer dailyDrivingMinutesToday = driver.getDailyDrivingMinutesToday();

        // Инициализация значений по умолчанию, если данные отсутствуют
        if (continuousDrivingMinutes == null) {
            continuousDrivingMinutes = 0;
        }

        if (dailyDrivingMinutesToday == null) {
            dailyDrivingMinutesToday = 0;
        }

        // Рассчитываем оставшееся время непрерывного вождения
        int remainingContinuousDrivingMinutes = maxContinuousDrivingMinutes - continuousDrivingMinutes;

        // Рассчитываем оставшееся время суточного вождения
        int remainingDailyDrivingMinutes = maxDailyDrivingMinutes - dailyDrivingMinutesToday;

        // Проверяем, не превысит ли маршрут лимит непрерывного вождения
        if (routeDurationMinutes > remainingContinuousDrivingMinutes) {
            warnings.add(String.format(
                    "Превышение лимита непрерывного вождения на %d минут. " +
                            "Необходим перерыв после %d минут вождения.",
                    routeDurationMinutes - remainingContinuousDrivingMinutes,
                    remainingContinuousDrivingMinutes
            ));
            result.setCompliant(false);
        }

        // Проверяем, не превысит ли маршрут лимит суточного вождения
        if (routeDurationMinutes > remainingDailyDrivingMinutes) {
            warnings.add(String.format(
                    "Превышение лимита суточного вождения на %d минут. " +
                            "Доступно только %d минут вождения в текущие сутки.",
                    routeDurationMinutes - remainingDailyDrivingMinutes,
                    remainingDailyDrivingMinutes
            ));
            result.setCompliant(false);
        }

        // Если водитель находится не в режиме вождения, проверяем минимальное время отдыха
        if (currentStatus != null && statusStartTime != null &&
                (currentStatus == DrivingStatus.REST_BREAK ||
                        currentStatus == DrivingStatus.DAILY_REST)) {

            Duration restDuration = Duration.between(statusStartTime, LocalDateTime.now());
            long restMinutes = restDuration.toMinutes();

            if (currentStatus == DrivingStatus.REST_BREAK &&
                    restMinutes < longBreakMinutes &&
                    continuousDrivingMinutes >= maxContinuousDrivingMinutes) {

                warnings.add(String.format(
                        "Недостаточное время отдыха. Требуется минимум %d минут непрерывного отдыха " +
                                "после достижения лимита непрерывного вождения.",
                        longBreakMinutes
                ));
                result.setCompliant(false);
            }
        }

        // Если нет предупреждений, маршрут соответствует нормативам РТО
        if (warnings.isEmpty()) {
            result.setCompliant(true);
        }

        result.setWarnings(warnings);
        return result;
    }
}

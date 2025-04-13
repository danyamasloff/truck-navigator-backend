package ru.maslov.trucknavigator.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Driver;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO с результатами анализа режима труда и отдыха водителя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRestAnalysisDto {

    // Информация о водителе
    private Long driverId;
    private String driverName;

    // Информация о поездке
    private LocalDateTime departureTime;
    private LocalDateTime estimatedArrivalTime;
    private int estimatedTripDurationMinutes;

    // Текущий статус водителя
    private Driver.DrivingStatus currentDrivingStatus;
    private LocalDateTime currentStatusStartTime;

    // Оставшееся время вождения
    private int remainingContinuousDrivingMinutes;
    private int remainingDailyDrivingMinutes;

    // Рекомендации по остановкам для отдыха
    private List<RestStopRecommendationDto> restStopRecommendations;

    // Соответствие нормативам
    private boolean compliantWithRegulations;

    // Текстовое резюме анализа
    private String summary;
}
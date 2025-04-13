package ru.maslov.trucknavigator.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO с рекомендацией по остановке для отдыха водителя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestStopRecommendationDto {

    // Расположение остановки на маршруте
    private double distanceFromStartKm;
    private String timeFromDeparture;  // Форматированное время от начала поездки (например "2ч 15мин")

    // Координаты остановки (если доступны)
    private double longitude;
    private double latitude;

    // Время прибытия в точку остановки
    private LocalDateTime expectedArrivalAtStop;

    // Информация о рекомендуемом отдыхе
    private int recommendedRestDurationMinutes;
    private String restType;           // Тип отдыха: "Короткий перерыв", "Длительный отдых", "Суточный отдых"
    private String reason;             // Причина остановки
}
package ru.maslov.trucknavigator.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
    private Double latitude;
    private Double longitude;

    // Время прибытия в точку остановки
    private LocalDateTime expectedArrivalAtStop;

    // Информация о рекомендуемом отдыхе
    private int recommendedRestDurationMinutes;
    private String restType;           // Тип отдыха: "Короткий перерыв", "Длительный отдых", "Суточный отдых"
    private String reason;             // Причина остановки

    // Информация о реальном месте остановки
    private String locationName;       // Название места (например "АЗС Лукойл")
    private String locationDescription; // Описание места
    private String locationType;       // Тип места (parking, rest_area, hotel, fuel и т.д.)
    private Double rating;             // Рейтинг места (если доступен)
    private Double distanceFromRoute;  // Расстояние отклонения от маршрута в метрах

    // Доступные услуги
    @Builder.Default
    private Map<String, Boolean> facilities = new HashMap<>();

    // Дополнительные экономические данные
    private Double fuelPrice;          // Цена топлива (если есть АЗС)
    private Double parkingCost;        // Стоимость парковки (если платная)
}

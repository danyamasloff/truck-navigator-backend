package ru.maslov.trucknavigator.dto.weather;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WeatherHazardWarningDto {
    private WeatherHazardType hazardType; // Тип погодной опасности
    private HazardSeverity severity;      // Серьезность опасности
    private double distanceFromStart;     // Расстояние от начала маршрута
    private LocalDateTime expectedTime;   // Ожидаемое время столкновения с опасностью
    private String description;           // Описание опасного явления
    private String recommendation;        // Рекомендация для водителя
}
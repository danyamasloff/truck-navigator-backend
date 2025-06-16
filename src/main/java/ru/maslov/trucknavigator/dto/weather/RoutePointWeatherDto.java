package ru.maslov.trucknavigator.dto.weather;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoutePointWeatherDto {
    private int pointIndex;           // Индекс точки в маршруте
    private double distanceFromStart; // Расстояние от начала маршрута в км
    private LocalDateTime estimatedTime; // Оценочное время прибытия в точку
    private WeatherDataDto weatherData;  // Погодные данные для этой точки
}

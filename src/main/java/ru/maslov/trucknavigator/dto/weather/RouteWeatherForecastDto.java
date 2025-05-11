package ru.maslov.trucknavigator.dto.weather;

import lombok.Data;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteWeatherForecastDto {
    private RouteResponseDto route;              // Базовая информация о маршруте
    private LocalDateTime departureTime;         // Время отправления
    private List<RoutePointWeatherDto> pointForecasts; // Прогнозы для точек маршрута
    private List<WeatherHazardWarningDto> hazardWarnings; // Предупреждения об опасностях
    private boolean hasHazardousConditions;      // Флаг наличия опасных условий
    private String summary;                      // Краткая сводка о погоде на маршруте
}
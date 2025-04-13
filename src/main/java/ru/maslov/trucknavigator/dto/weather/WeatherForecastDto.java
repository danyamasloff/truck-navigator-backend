package ru.maslov.trucknavigator.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO с прогнозом погоды, содержащий список прогнозов на различные временные точки.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecastDto {

    private String cityName;
    private String cityCountry;
    private List<WeatherDataDto> forecasts = new ArrayList<>();

    /**
     * Получает прогноз погоды для конкретного времени из списка прогнозов.
     * Возвращает ближайший прогноз, если точное совпадение не найдено.
     *
     * @param hour час (0-23)
     * @param day день (начиная от 0 - сегодня, 1 - завтра и т.д.)
     * @return прогноз погоды или null, если прогноз не найден
     */
    public WeatherDataDto getForecastFor(int hour, int day) {
        if (forecasts == null || forecasts.isEmpty()) {
            return null;
        }

        // Находим ближайший прогноз по времени
        WeatherDataDto closestForecast = null;
        long closestDifference = Long.MAX_VALUE;

        for (WeatherDataDto forecast : forecasts) {
            if (forecast.getForecastTime() != null) {
                // Вычисляем разницу во времени
                long difference = Math.abs(
                        forecast.getForecastTime().getHour() - hour +
                                (forecast.getForecastTime().getDayOfYear() - java.time.LocalDateTime.now().getDayOfYear() - day) * 24
                );

                if (difference < closestDifference) {
                    closestDifference = difference;
                    closestForecast = forecast;
                }
            }
        }

        return closestForecast;
    }
}
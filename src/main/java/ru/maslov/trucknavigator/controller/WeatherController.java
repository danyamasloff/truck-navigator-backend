package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.dto.weather.RouteWeatherForecastDto;
import ru.maslov.trucknavigator.dto.weather.WeatherDataDto;
import ru.maslov.trucknavigator.dto.weather.WeatherForecastDto;
import ru.maslov.trucknavigator.dto.weather.WeatherHazardWarningDto;
import ru.maslov.trucknavigator.integration.openweather.WeatherService;
import ru.maslov.trucknavigator.service.RouteWeatherService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Погода", description = "API для получения погодных данных")
public class WeatherController {

    private final WeatherService weatherService;
    private final RouteWeatherService routeWeatherService;

    @GetMapping("/current")
    @Operation(summary = "Получить текущую погоду",
            description = "Возвращает текущую погоду для указанных координат")
    public ResponseEntity<WeatherDataDto> getCurrentWeather(
            @RequestParam double lat,
            @RequestParam double lon) {
        return ResponseEntity.ok(weatherService.getCurrentWeather(lat, lon));
    }

    @GetMapping("/forecast")
    @Operation(summary = "Получить прогноз погоды",
            description = "Возвращает прогноз погоды для указанных координат")
    public ResponseEntity<WeatherForecastDto> getWeatherForecast(
            @RequestParam double lat,
            @RequestParam double lon) {
        return ResponseEntity.ok(weatherService.getWeatherForecast(lat, lon));
    }

    @PostMapping("/route-forecast")
    @Operation(summary = "Получить прогноз погоды для маршрута",
            description = "Анализирует погодные условия вдоль маршрута с учетом времени движения")
    public ResponseEntity<RouteWeatherForecastDto> getRouteWeatherForecast(
            @RequestBody RouteResponseDto route,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureTime) {

        RouteWeatherForecastDto forecast =
                routeWeatherService.generateRouteWeatherForecast(route, departureTime);

        if (forecast == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(forecast);
    }

    @PostMapping("/hazard-warnings")
    @Operation(summary = "Получить предупреждения о погодных опасностях",
            description = "Возвращает список предупреждений о погодных опасностях на маршруте")
    public ResponseEntity<List<WeatherHazardWarningDto>> getHazardWarnings(
            @RequestBody RouteResponseDto route,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureTime) {

        RouteWeatherForecastDto forecast =
                routeWeatherService.generateRouteWeatherForecast(route, departureTime);

        if (forecast == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(forecast.getHazardWarnings());
    }
}
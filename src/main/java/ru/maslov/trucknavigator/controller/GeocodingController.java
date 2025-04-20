package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.geocoding.GeoLocationDto;
import ru.maslov.trucknavigator.dto.geocoding.GeoPoint;
import ru.maslov.trucknavigator.integration.graphhopper.GeocodingService;

import java.util.List;

/**
 * Контроллер для операций геокодирования и поиска мест.
 * Предоставляет API для преобразования текстовых адресов в координаты
 * и поиска различных типов мест на карте.
 */
@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Геокодирование", description = "API для преобразования адресов в координаты и поиска мест")
public class GeocodingController {

    private final GeocodingService geocodingService;

    /**
     * Поиск координат по текстовому названию места или адресу.
     *
     * @param placeName Название места или адрес
     * @return Координаты места (широта, долгота)
     */
    @GetMapping("/geocode")
    @Operation(summary = "Геокодировать место",
            description = "Преобразует текстовый адрес или название места в географические координаты")
    public ResponseEntity<GeoPoint> geocodePlace(
            @Parameter(description = "Название места или адрес")
            @RequestParam String placeName) {

        log.info("Запрос на геокодирование: {}", placeName);
        GeoPoint point = geocodingService.geocode(placeName);
        return ResponseEntity.ok(point);
    }

    /**
     * Поиск мест по текстовому запросу с поддержкой фильтров.
     *
     * @param query Поисковый запрос
     * @param osmTag OSM-тег для фильтрации (например, amenity:fuel для АЗС)
     * @param limit Максимальное количество результатов
     * @param lat Опорная широта для поиска ближайших мест
     * @param lon Опорная долгота для поиска ближайших мест
     * @return Список найденных мест с информацией
     */
    @GetMapping("/search")
    @Operation(summary = "Поиск мест",
            description = "Ищет места по текстовому запросу с возможностью фильтрации по типу")
    public ResponseEntity<List<GeoLocationDto>> searchPlaces(
            @Parameter(description = "Поисковый запрос")
            @RequestParam(required = false) String query,

            @Parameter(description = "OSM-тег для фильтрации (например, amenity:fuel для АЗС, shop:supermarket для супермаркетов)")
            @RequestParam(required = false) String osmTag,

            @Parameter(description = "Максимальное количество результатов")
            @RequestParam(required = false, defaultValue = "5") Integer limit,

            @Parameter(description = "Опорная широта для поиска ближайших мест")
            @RequestParam(required = false) Double lat,

            @Parameter(description = "Опорная долгота для поиска ближайших мест")
            @RequestParam(required = false) Double lon) {

        log.info("Запрос на поиск мест: запрос={}, тег={}, рядом с={},{}",
                query, osmTag, lat, lon);

        List<GeoLocationDto> results = geocodingService.searchPlaces(query, osmTag, limit, lat, lon);
        return ResponseEntity.ok(results);
    }

    /**
     * Обратное геокодирование: определение адреса по координатам.
     *
     * @param lat Широта
     * @param lon Долгота
     * @return Информация о месте по указанным координатам
     */
    @GetMapping("/reverse")
    @Operation(summary = "Обратное геокодирование",
            description = "Определяет адрес и информацию о месте по координатам")
    public ResponseEntity<GeoLocationDto> reverseGeocode(
            @Parameter(description = "Широта")
            @RequestParam double lat,

            @Parameter(description = "Долгота")
            @RequestParam double lon) {

        log.info("Запрос на обратное геокодирование: {},{}", lat, lon);
        GeoLocationDto location = geocodingService.reverseGeocode(lat, lon);
        return ResponseEntity.ok(location);
    }

    /**
     * Поиск заправок (АЗС) рядом с указанной точкой.
     *
     * @param lat Широта
     * @param lon Долгота
     * @param radius Радиус поиска в метрах (необязательно)
     * @return Список найденных АЗС
     */
    @GetMapping("/fuel-stations")
    @Operation(summary = "Поиск АЗС",
            description = "Ищет автозаправочные станции рядом с указанными координатами")
    public ResponseEntity<List<GeoLocationDto>> findFuelStations(
            @Parameter(description = "Широта")
            @RequestParam double lat,

            @Parameter(description = "Долгота")
            @RequestParam double lon,

            @Parameter(description = "Радиус поиска в метрах")
            @RequestParam(required = false, defaultValue = "5000") Integer radius) {

        log.info("Запрос на поиск АЗС: {},{}, радиус: {}м", lat, lon, radius);
        List<GeoLocationDto> stations = geocodingService.findNearbyFuelStations(lat, lon, radius);
        return ResponseEntity.ok(stations);
    }

    /**
     * Поиск мест отдыха для водителей (кафе, рестораны, придорожные сервисы) рядом с указанной точкой.
     *
     * @param lat Широта
     * @param lon Долгота
     * @return Список найденных мест отдыха
     */
    @GetMapping("/rest-areas")
    @Operation(summary = "Поиск мест отдыха",
            description = "Ищет рестораны, кафе и придорожные сервисы рядом с указанными координатами")
    public ResponseEntity<List<GeoLocationDto>> findRestAreas(
            @Parameter(description = "Широта")
            @RequestParam double lat,

            @Parameter(description = "Долгота")
            @RequestParam double lon) {

        log.info("Запрос на поиск мест отдыха: {},{}", lat, lon);

        // Для поиска мест отдыха: используем специализированный метод
        List<GeoLocationDto> restAreas = geocodingService.findPOIsByType(lat, lon, "restaurant", null);

        // Дополняем специальными местами отдыха на трассе
        List<GeoLocationDto> restStops = geocodingService.findPOIsByType(lat, lon, "rest_area", null);
        restAreas.addAll(restStops);

        return ResponseEntity.ok(restAreas);
    }
}
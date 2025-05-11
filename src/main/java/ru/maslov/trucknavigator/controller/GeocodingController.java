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

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Геокодирование", description = "API для геокодирования и поиска POI")
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping("/geocode")
    @Operation(summary = "Геокодировать место")
    public ResponseEntity<GeoPoint> geocode(
            @Parameter(description = "Название или адрес") @RequestParam String placeName) {
        log.info("Geocode request: {}", placeName);
        return ResponseEntity.ok(geocodingService.geocode(placeName));
    }

    @GetMapping("/reverse")
    @Operation(summary = "Обратное геокодирование")
    public ResponseEntity<GeoLocationDto> reverse(
            @Parameter(description = "Широта") @RequestParam double lat,
            @Parameter(description = "Долгота") @RequestParam double lon) {
        log.info("Reverse geocode: {},{}", lat, lon);
        return ResponseEntity.ok(geocodingService.reverseGeocode(lat, lon));
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск мест по запросу")
    public ResponseEntity<List<GeoLocationDto>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String osmTag,
            @RequestParam(required = false, defaultValue = "5") Integer limit,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {
        log.info("Search places: {}, tag={}, near={}/{}", query, osmTag, lat, lon);
        return ResponseEntity.ok(geocodingService.searchPlaces(query, osmTag, limit, lat, lon));
    }

    @GetMapping("/fuel-stations")
    @Operation(summary = "Поиск АЗС")
    public ResponseEntity<List<GeoLocationDto>> fuel(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "5000") Integer radius) {
        log.info("Fuel stations: {},{}, radius {}m", lat, lon, radius);
        return ResponseEntity.ok(geocodingService.findFuelStations(lat, lon, radius));
    }

    @GetMapping("/rest-areas")
    @Operation(summary = "Поиск зон отдыха")
    public ResponseEntity<List<GeoLocationDto>> rest(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "15000") Integer radius) {
        log.info("Rest areas: {},{}, radius {}m", lat, lon, radius);
        return ResponseEntity.ok(geocodingService.findRestAreas(lat, lon, radius));
    }

    @GetMapping("/food-stops")
    @Operation(summary = "Поиск кафе и ресторанов")
    public ResponseEntity<List<GeoLocationDto>> food(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "10000") Integer radius) {
        log.info("Food stops: {},{}, radius {}m", lat, lon, radius);
        return ResponseEntity.ok(geocodingService.findFoodStops(lat, lon, radius));
    }

    @GetMapping("/parking")
    @Operation(summary = "Поиск парковок")
    public ResponseEntity<List<GeoLocationDto>> parking(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "10000") Integer radius) {
        log.info("Parking spots: {},{}, radius {}m", lat, lon, radius);
        return ResponseEntity.ok(geocodingService.findParkingSpots(lat, lon, radius));
    }

    @GetMapping("/lodging")
    @Operation(summary = "Поиск отелей и мотелей")
    public ResponseEntity<List<GeoLocationDto>> lodging(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "20000") Integer radius) {
        log.info("Lodging: {},{}, radius {}m", lat, lon, radius);
        return ResponseEntity.ok(geocodingService.findLodging(lat, lon, radius));
    }

    @GetMapping("/atms")
    @Operation(summary = "Поиск банкоматов")
    public ResponseEntity<List<GeoLocationDto>> atms(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "5000") Integer radius) {
        log.info("ATMs: {},{}, radius {}m", lat, lon, radius);
        return ResponseEntity.ok(geocodingService.findAtms(lat, lon, radius));
    }

    @GetMapping("/pharmacies")
    public ResponseEntity<List<GeoLocationDto>> pharmacies(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "5000") Integer radius) {
        return ResponseEntity.ok(
                geocodingService.findPharmacies(lat, lon, radius));
    }

    @GetMapping("/hospitals")
    public ResponseEntity<List<GeoLocationDto>> hospitals(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false, defaultValue = "10000") Integer radius) {
        return ResponseEntity.ok(
                geocodingService.findHospitals(lat, lon, radius));
    }

}

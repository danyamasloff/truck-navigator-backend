package ru.maslov.trucknavigator.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для запроса построения маршрута.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequestDto {

    private String name;

    // Начальная точка
    private double startLat;
    private double startLon;
    private String startAddress;

    // Конечная точка
    private double endLat;
    private double endLon;
    private String endAddress;

    // Промежуточные точки (waypoints)
    private List<WaypointDto> waypoints = new ArrayList<>();

    // GraphHopper API параметры
    private String profile;
    private boolean calcPoints;
    private boolean instructions;
    private boolean pointsEncoded;

    // Идентификаторы связанных сущностей
    private Long vehicleId;
    private Long driverId;
    private Long cargoId;

    // Время отправления
    private LocalDateTime departureTime;

    // Дополнительные параметры
    private boolean avoidTolls;
    private boolean avoidHighways;
    private boolean avoidUrbanAreas;
    private boolean considerWeather;
    private boolean considerTraffic;

    /**
     * DTO для промежуточной точки в запросе маршрута.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaypointDto {
        private double latitude;
        private double longitude;
        private String address;
        private String name;
        private String waypointType;
        private boolean stopover;
        private int stopDuration;
    }
}

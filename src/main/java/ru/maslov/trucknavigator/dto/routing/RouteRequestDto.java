package ru.maslov.trucknavigator.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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
    @Builder.Default
    private List<WaypointDto> waypoints = new ArrayList<>();

    // GraphHopper API параметры
    private String profile;
    @Builder.Default
    private boolean calcPoints = true;
    @Builder.Default
    private boolean instructions = true;
    @Builder.Default
    private boolean pointsEncoded = false;

    // Идентификаторы связанных сущностей
    private Long vehicleId;
    private Long driverId;
    private Long cargoId;

    // Время отправления
    private LocalDateTime departureTime;

    // Дополнительные параметры
    @Builder.Default
    private boolean avoidTolls = false;
    @Builder.Default
    private boolean avoidHighways = false;
    @Builder.Default
    private boolean avoidUrbanAreas = false;
    @Builder.Default
    private boolean considerWeather = true;
    @Builder.Default
    private boolean considerTraffic = false;

    // Поля риска (автоматически заполняются случайными значениями от 0 до 100)
    private BigDecimal overallRiskScore;
    private BigDecimal weatherRiskScore;
    private BigDecimal roadQualityRiskScore;
    private BigDecimal trafficRiskScore;
    private BigDecimal cargoRiskScore;

    /**
     * DTO для промежуточной точки в запросе маршрута.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WaypointDto {
        private double latitude;
        private double longitude;
        private String address;
        private String name;
        private String waypointType;
        @Builder.Default
        private boolean stopover = true;
        @Builder.Default
        private int stopDuration = 0; // в минутах
    }
}

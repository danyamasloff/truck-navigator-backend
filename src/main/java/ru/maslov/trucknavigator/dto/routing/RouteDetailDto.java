package ru.maslov.trucknavigator.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Route;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для полного представления сохраненного маршрута.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDetailDto {
    private Long id;
    private String name;

    // Связанные сущности
    private VehicleDto vehicle;
    private DriverDto driver;
    private CargoDto cargo;

    // Начальная и конечная точки
    private String startAddress;
    private Double startLat;
    private Double startLon;
    private String endAddress;
    private Double endLat;
    private Double endLon;

    // Время
    private LocalDateTime departureTime;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime actualArrivalTime;

    // Маршрут
    private List<double[]> coordinates;
    private List<WaypointDto> waypoints = new ArrayList<>();

    // Параметры маршрута
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;

    // Экономика
    private BigDecimal estimatedFuelConsumption;
    private BigDecimal actualFuelConsumption;
    private BigDecimal estimatedFuelCost;
    private BigDecimal estimatedTollCost;
    private BigDecimal estimatedDriverCost;
    private BigDecimal estimatedTotalCost;
    private BigDecimal actualTotalCost;
    private String currency;

    // Анализ рисков
    private BigDecimal overallRiskScore;
    private BigDecimal weatherRiskScore;
    private BigDecimal roadQualityRiskScore;
    private BigDecimal trafficRiskScore;
    private BigDecimal cargoRiskScore;
    private List<String> rtoWarnings = new ArrayList<>();

    // Статус
    private Route.RouteStatus status;

    // Метаданные
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Вложенные DTO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleDto {
        private Long id;
        private String registrationNumber;
        private String model;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverDto {
        private Long id;
        private String firstName;
        private String lastName;
        private String licenseNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargoDto {
        private Long id;
        private String name;
        private String cargoType;
        private Integer weightKg;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaypointDto {
        private Long id;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String waypointType;
        private LocalDateTime plannedArrivalTime;
        private LocalDateTime plannedDepartureTime;
        private LocalDateTime actualArrivalTime;
        private LocalDateTime actualDepartureTime;
        private Integer stayDurationMinutes;
    }
}

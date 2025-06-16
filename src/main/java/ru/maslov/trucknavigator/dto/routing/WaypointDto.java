package ru.maslov.trucknavigator.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для работы с промежуточными точками маршрута.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaypointDto {
    private Long id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String type; // тип точки (WAYPOINT, PICKUP, DELIVERY, FUEL, etc.)
    private Integer orderIndex; // порядковый номер в маршруте

    // Временные параметры
    private LocalDateTime plannedArrivalTime;
    private LocalDateTime plannedDepartureTime;
    private LocalDateTime actualArrivalTime;
    private LocalDateTime actualDepartureTime;
    private Integer stayDurationMinutes;

    // Дополнительная информация
    private String additionalInfo;

    // Служебные поля
    private Long routeId;
}

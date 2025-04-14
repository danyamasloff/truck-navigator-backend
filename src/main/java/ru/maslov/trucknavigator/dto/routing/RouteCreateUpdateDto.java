package ru.maslov.trucknavigator.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для создания и обновления маршрута.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteCreateUpdateDto {
    private Long id; // null для создания, не null для обновления

    @NotBlank(message = "Название маршрута обязательно")
    private String name;

    // Связи с другими сущностями
    private Long vehicleId;
    private Long driverId;
    private Long cargoId;

    // Начальная и конечная точки
    @NotBlank(message = "Начальный адрес обязателен")
    private String startAddress;

    @NotNull(message = "Начальная широта обязательна")
    private Double startLat;

    @NotNull(message = "Начальная долгота обязательна")
    private Double startLon;

    @NotBlank(message = "Конечный адрес обязателен")
    private String endAddress;

    @NotNull(message = "Конечная широта обязательна")
    private Double endLat;

    @NotNull(message = "Конечная долгота обязательна")
    private Double endLon;

    // Время отправления
    private LocalDateTime departureTime;

    // Промежуточные точки
    private List<WaypointDto> waypoints = new ArrayList<>();

    // Статус маршрута
    private Route.RouteStatus status;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WaypointDto {
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String waypointType;
        private Integer stayDurationMinutes;
    }
}
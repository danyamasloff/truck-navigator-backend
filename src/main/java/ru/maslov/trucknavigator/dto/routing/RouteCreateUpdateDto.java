package ru.maslov.trucknavigator.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Route;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для создания и обновления маршрута.
 * Включает все поля из сущности Route для полного контроля над данными.
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

    // Время отправления и прибытия
    private LocalDateTime departureTime;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime actualArrivalTime;

    // Промежуточные точки
    @Builder.Default
    private List<WaypointDto> waypoints = new ArrayList<>();

    // Параметры маршрута (могут быть рассчитаны автоматически)
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal estimatedFuelConsumption;
    private BigDecimal actualFuelConsumption;

    // Экономические показатели (могут быть рассчитаны автоматически)
    private BigDecimal estimatedFuelCost;
    private BigDecimal estimatedTollCost;
    private BigDecimal estimatedDriverCost;
    private BigDecimal estimatedTotalCost;
    private BigDecimal actualTotalCost;
    private String currency;

    // Анализ рисков (могут быть рассчитаны автоматически)
    private BigDecimal overallRiskScore; // 0-100
    private BigDecimal weatherRiskScore; // 0-100
    private BigDecimal roadQualityRiskScore; // 0-100
    private BigDecimal trafficRiskScore; // 0-100
    private BigDecimal cargoRiskScore; // 0-100

    // Статус маршрута
    private Route.RouteStatus status;

    // Флаги для автоматического расчета (используются только при создании)
    @Builder.Default
    private Boolean autoCalculateRoute = true; // Автоматически рассчитать маршрут
    @Builder.Default
    private Boolean autoCalculateEconomics = true; // Автоматически рассчитать экономику
    @Builder.Default
    private Boolean autoCalculateRisks = true; // Автоматически рассчитать риски

    // Дополнительные параметры для расчета
    private Boolean avoidTolls;
    private Boolean considerWeather;
    private Boolean considerTraffic;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WaypointDto {
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String waypointType;
        private Integer stayDurationMinutes;
    }
}

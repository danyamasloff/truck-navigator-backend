package ru.maslov.trucknavigator.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Route;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO для отображения маршрута в списках.
 * Содержит только основные данные без детальной информации.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteSummaryDto {
    private Long id;
    private String name;
    private String startAddress;
    private String endAddress;
    private LocalDateTime departureTime;
    private LocalDateTime estimatedArrivalTime;
    private BigDecimal distanceKm;
    private Integer estimatedDurationMinutes;
    private String vehicleName;
    private String driverName;
    private String cargoName;
    private BigDecimal estimatedTotalCost;
    private Route.RouteStatus status;
    private BigDecimal overallRiskScore;

    /**
     * Форматирует время в удобочитаемый формат.
     */
    public String getFormattedDepartureTime() {
        return departureTime != null
                ? departureTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "";
    }

    /**
     * Форматирует продолжительность в удобочитаемый формат.
     */
    public String getFormattedDuration() {
        if (estimatedDurationMinutes == null) return "";

        int hours = estimatedDurationMinutes / 60;
        int minutes = estimatedDurationMinutes % 60;

        return hours > 0
                ? String.format("%dч %dмин", hours, minutes)
                : String.format("%dмин", minutes);
    }
}
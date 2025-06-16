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

    // Связанные сущности (имена и ID)
    private String vehicleName;
    private Long vehicleId;
    private String driverName;
    private Long driverId;
    private String cargoName;
    private Long cargoId;

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

    /**
     * Преобразует сводную информацию в детальную DTO.
     */
    public RouteDetailDto toDetailDto() {
        RouteDetailDto detailDto = new RouteDetailDto();
        detailDto.setId(this.id);
        detailDto.setName(this.name);
        detailDto.setStartAddress(this.startAddress);
        detailDto.setEndAddress(this.endAddress);
        detailDto.setDepartureTime(this.departureTime);
        detailDto.setEstimatedArrivalTime(this.estimatedArrivalTime);
        detailDto.setDistanceKm(this.distanceKm);
        detailDto.setEstimatedDurationMinutes(this.estimatedDurationMinutes);
        detailDto.setEstimatedTotalCost(this.estimatedTotalCost);
        detailDto.setStatus(this.status);
        detailDto.setOverallRiskScore(this.overallRiskScore);

        // Связанные сущности добавляются отдельно

        return detailDto;
    }
}

package ru.maslov.trucknavigator.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для экономического анализа маршрута.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteEconomicsDto {
    private Long routeId;
    private BigDecimal fuelCost;
    private BigDecimal driverCost;
    private BigDecimal tollRoadsCost;
    private BigDecimal vehicleDepreciation;
    private BigDecimal maintenanceCost;
    private BigDecimal totalCost;
    private BigDecimal costPerKm;
    private String currency;
    private LocalDateTime calculationTime;
}

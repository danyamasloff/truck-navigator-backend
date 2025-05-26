package ru.maslov.trucknavigator.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для анализа расхода топлива.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelConsumptionAnalysisDto {
    private BigDecimal baseConsumptionPer100Km;
    private BigDecimal adjustedConsumptionPer100Km;
    private BigDecimal totalFuelConsumption;
    private BigDecimal cargoWeightImpact;
    private BigDecimal weatherImpact;
    private BigDecimal roadSlopeImpact;
    private BigDecimal totalDistance;
    private String analysisExplanation;
}
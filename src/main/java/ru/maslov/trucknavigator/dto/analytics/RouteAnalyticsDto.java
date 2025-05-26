package ru.maslov.trucknavigator.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Комплексный DTO для аналитики маршрута.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteAnalyticsDto {
    private RouteEconomicsDto economics;
    private FuelConsumptionAnalysisDto fuelConsumption;
    private DriverRestAnalysisDto driverRest;
    private List<RiskFactorDto> riskFactors = new ArrayList<>();
    private WeatherAnalysisDto weatherAnalysis;
    private Boolean isCompliant;
    private String summary;
}
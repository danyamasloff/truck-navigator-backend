package ru.maslov.trucknavigator.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFactorDto {
    private String category; // ROUTE, VEHICLE, DRIVER, CARGO, WEATHER
    private String type;
    private String description;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private int riskScore; // 0-100
    private String recommendation;
}
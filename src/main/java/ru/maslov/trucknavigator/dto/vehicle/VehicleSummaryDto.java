package ru.maslov.trucknavigator.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для отображения транспортного средства в списках.
 * Содержит только основные данные без детальной информации.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSummaryDto {
    private Long id;
    private String registrationNumber;
    private String model;
    private String manufacturer;
    private Integer productionYear;
    private Integer heightCm;
    private Integer widthCm;
    private Integer lengthCm;
    private Integer maxLoadCapacityKg;
    private String engineType;
    private BigDecimal fuelConsumptionPer100km;
    private BigDecimal currentFuelLevelLitres;
    private BigDecimal currentOdometerKm;

    /**
     * Возвращает полное название ТС для отображения в списках
     */
    public String getFullName() {
        return model + " (" + registrationNumber + ")";
    }
}
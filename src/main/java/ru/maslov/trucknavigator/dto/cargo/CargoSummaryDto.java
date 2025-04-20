package ru.maslov.trucknavigator.dto.cargo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Cargo;

import java.math.BigDecimal;

/**
 * DTO для отображения груза в списках.
 * Содержит только основные данные без детальной информации.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoSummaryDto {
    private Long id;
    private String name;
    private String description;
    private Integer weightKg;
    private Cargo.CargoType cargoType;
    private boolean isFragile;
    private boolean isPerishable;
    private boolean isDangerous;
    private boolean isOversized;
    private boolean requiresTemperatureControl;
    private BigDecimal declaredValue;
    private String currency;
}
package ru.maslov.trucknavigator.dto.cargo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Cargo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для детального представления груза.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoDetailDto {
    private Long id;
    private String name;
    private String description;

    // Характеристики груза
    private Integer weightKg;
    private BigDecimal volumeCubicMeters;
    private Integer lengthCm;
    private Integer widthCm;
    private Integer heightCm;

    // Тип груза
    private Cargo.CargoType cargoType;

    // Специальные требования и ограничения
    private boolean isFragile;
    private boolean isPerishable;
    private boolean isDangerous;
    private String dangerousGoodsClass;
    private String unNumber;
    private boolean isOversized;
    private boolean requiresTemperatureControl;
    private Integer minTemperatureCelsius;
    private Integer maxTemperatureCelsius;
    private boolean requiresCustomsClearance;

    // Стоимость груза
    private BigDecimal declaredValue;
    private String currency;

    // Метаданные
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

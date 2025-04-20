package ru.maslov.trucknavigator.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для детального представления транспортного средства.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDetailDto {
    private Long id;
    private String registrationNumber;
    private String model;
    private String manufacturer;
    private Integer productionYear;

    // Габариты транспортного средства
    private Integer heightCm;
    private Integer widthCm;
    private Integer lengthCm;

    // Вес и грузоподъемность
    private Integer emptyWeightKg;
    private Integer maxLoadCapacityKg;
    private Integer grossWeightKg;

    // Параметры двигателя и расхода топлива
    private String engineType; // DIESEL, PETROL, ELECTRIC, HYBRID
    private Integer fuelCapacityLitres;
    private BigDecimal fuelConsumptionPer100km;

    // Экологический класс
    private String emissionClass; // EURO_3, EURO_4, EURO_5, EURO_6

    // Тип транспортного средства по конфигурации осей
    private String axisConfiguration; // 4X2, 6X4, 8X4 и т.д.
    private Integer axisCount;

    // Специальные характеристики
    private boolean hasRefrigerator;
    private boolean hasDangerousGoodsPermission;
    private boolean hasOversizedCargoPermission;

    // Текущее состояние ТС
    private BigDecimal currentFuelLevelLitres;
    private BigDecimal currentOdometerKm;

    // Метаданные
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
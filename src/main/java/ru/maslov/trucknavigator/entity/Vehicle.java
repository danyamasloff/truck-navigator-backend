package ru.maslov.trucknavigator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сущность, представляющая грузовое транспортное средство.
 * Содержит все характеристики, необходимые для расчета маршрутов и ограничений.
 */
@Entity
@Table(name = "vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "production_year")
    private Integer productionYear;

    // Габариты транспортного средства
    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    @Column(name = "width_cm", nullable = false)
    private Integer widthCm;

    @Column(name = "length_cm", nullable = false)
    private Integer lengthCm;

    // Вес и грузоподъемность
    @Column(name = "empty_weight_kg", nullable = false)
    private Integer emptyWeightKg;

    @Column(name = "max_load_capacity_kg", nullable = false)
    private Integer maxLoadCapacityKg;

    @Column(name = "gross_weight_kg", nullable = false)
    private Integer grossWeightKg;

    // Параметры двигателя и расхода топлива
    @Column(name = "engine_type")
    private String engineType; // DIESEL, PETROL, ELECTRIC, HYBRID

    @Column(name = "fuel_capacity_litres")
    private Integer fuelCapacityLitres;

    @Column(name = "fuel_consumption_per_100km")
    private BigDecimal fuelConsumptionPer100km;

    // Экологический класс
    @Column(name = "emission_class")
    private String emissionClass; // EURO_3, EURO_4, EURO_5, EURO_6

    // Тип транспортного средства по конфигурации осей
    @Column(name = "axis_configuration")
    private String axisConfiguration; // 4X2, 6X4, 8X4 и т.д.

    @Column(name = "axis_count")
    private Integer axisCount;

    // Специальные характеристики
    @Column(name = "has_refrigerator")
    private boolean hasRefrigerator;

    @Column(name = "has_dangerous_goods_permission")
    private boolean hasDangerousGoodsPermission;

    @Column(name = "has_oversized_cargo_permission")
    private boolean hasOversizedCargoPermission;

    // Текущее состояние ТС
    @Column(name = "current_fuel_level_litres")
    private BigDecimal currentFuelLevelLitres;

    @Column(name = "current_odometer_km")
    private BigDecimal currentOdometerKm;

    // Метаданные для аудита
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Добавленные поля для технического состояния
    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "maintenance_interval_km")
    private Integer maintenanceIntervalKm;

    // Эксплуатационные характеристики
    @Column(name = "avg_speed_kmh")
    private Double avgSpeedKmh;

    @Column(name = "avg_idle_time_percent")
    private Double avgIdleTimePercent;

    @Column(name = "actual_fuel_consumption_per_100km")
    private BigDecimal actualFuelConsumptionPer100km;

    @Column(name = "adr_certificate_number")
    private String adrCertificateNumber;

    @Column(name = "adr_certificate_expiry_date")
    private LocalDate adrCertificateExpiryDate;

    // Дополнительные характеристики для аналитики
    @Column(name = "avg_maintenance_cost_per_km")
    private BigDecimal avgMaintenanceCostPerKm;

    @Column(name = "vehicle_depreciation_per_km")
    private BigDecimal vehicleDepreciationPerKm;
}

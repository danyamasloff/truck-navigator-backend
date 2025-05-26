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
 * Сущность для хранения экономических параметров перевозок.
 * Используется для расчета стоимости маршрутов и других экономических показателей.
 */
@Entity
@Table(name = "cost_parameters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostParameters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parameter_name", nullable = false)
    private String parameterName;

    @Column(name = "fuel_price_per_liter", precision = 10, scale = 2)
    private BigDecimal fuelPricePerLiter;

    @Column(name = "driver_cost_per_hour", precision = 10, scale = 2)
    private BigDecimal driverCostPerHour;

    @Column(name = "driver_cost_per_km", precision = 10, scale = 2)
    private BigDecimal driverCostPerKm;

    @Column(name = "rest_stop_cost", precision = 10, scale = 2)
    private BigDecimal restStopCost;

    @Column(name = "vehicle_depreciation_per_km", precision = 10, scale = 2)
    private BigDecimal vehicleDepreciationPerKm;

    @Column(name = "toll_road_average_cost_per_km", precision = 10, scale = 2)
    private BigDecimal tollRoadAverageCostPerKm;

    @Column(name = "maintenance_cost_per_km", precision = 10, scale = 2)
    private BigDecimal maintenanceCostPerKm;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "region_code")
    private String regionCode;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

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
}
package ru.maslov.trucknavigator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность, представляющая груз для перевозки.
 * Содержит информацию о типе, весе, габаритах и особых требованиях к перевозке.
 */
@Entity
@Table(name = "cargos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    // Характеристики груза
    @Column(name = "weight_kg", nullable = false)
    private Integer weightKg;

    @Column(name = "volume_cubic_meters")
    private BigDecimal volumeCubicMeters;

    @Column(name = "length_cm")
    private Integer lengthCm;

    @Column(name = "width_cm")
    private Integer widthCm;

    @Column(name = "height_cm")
    private Integer heightCm;

    // Тип груза
    @Column(name = "cargo_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CargoType cargoType;

    // Специальные требования и ограничения
    @Column(name = "is_fragile")
    private boolean isFragile;

    @Column(name = "is_perishable")
    private boolean isPerishable;

    @Column(name = "is_dangerous")
    private boolean isDangerous;

    @Column(name = "dangerous_goods_class")
    private String dangerousGoodsClass; // ADR классы: 1, 2, 3 и т.д.

    @Column(name = "un_number")
    private String unNumber; // Номер ООН для опасных грузов

    @Column(name = "is_oversized")
    private boolean isOversized;

    @Column(name = "requires_temperature_control")
    private boolean requiresTemperatureControl;

    @Column(name = "min_temperature_celsius")
    private Integer minTemperatureCelsius;

    @Column(name = "max_temperature_celsius")
    private Integer maxTemperatureCelsius;

    @Column(name = "requires_customs_clearance")
    private boolean requiresCustomsClearance;

    // Стоимость груза (для страховки и оценки рисков)
    @Column(name = "declared_value")
    private BigDecimal declaredValue;

    @Column(name = "currency")
    private String currency;

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

    /**
     * Типы грузов
     */
    public enum CargoType {
        GENERAL,            // Генеральные грузы
        BULK,               // Навалочные грузы
        LIQUID,             // Жидкие грузы
        CONTAINER,          // Контейнерные грузы
        REFRIGERATED,       // Рефрижераторные грузы
        DANGEROUS,          // Опасные грузы
        OVERSIZED,          // Негабаритные грузы
        HEAVY,              // Тяжеловесные грузы
        LIVESTOCK,          // Животные
        PERISHABLE,         // Скоропортящиеся грузы
        VALUABLE,           // Ценные грузы
        FRAGILE             // Хрупкие грузы
    }
}
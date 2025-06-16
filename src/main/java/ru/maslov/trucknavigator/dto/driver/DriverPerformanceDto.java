package ru.maslov.trucknavigator.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverPerformanceDto {
    private Long driverId;
    private String driverName;
    private LocalDateTime analyzedPeriodStart;
    private LocalDateTime analyzedPeriodEnd;

    // Основные показатели
    private Integer completedRoutesCount;
    private BigDecimal totalDistanceDrivenKm;
    private BigDecimal avgFuelEfficiencyPercent;
    private BigDecimal avgDeliveryTimeEfficiencyPercent;
    private BigDecimal rating;
    private Integer incidentsCount;

    // Расширенная статистика
    private BigDecimal avgSpeedKmh;
    private BigDecimal totalFuelConsumptionLiters;
    private BigDecimal avgFuelConsumptionPer100km;
    private Long totalDrivingMinutes;
    private Long totalRestMinutes;

    // Сравнение с другими водителями
    private Integer rankingByEfficiency;
    private BigDecimal percentileByEfficiency;

    // История показателей
    private List<PerformanceHistoryPoint> performanceHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceHistoryPoint {
        private LocalDateTime date;
        private BigDecimal fuelEfficiency;
        private BigDecimal timeEfficiency;
        private BigDecimal rating;
    }
}

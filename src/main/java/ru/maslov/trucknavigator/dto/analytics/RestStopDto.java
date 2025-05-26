package ru.maslov.trucknavigator.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO для представления точки остановки для отдыха.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestStopDto {
    private Long minutesFromStart;
    private BigDecimal distanceFromStart;
    private String description;
    private Integer durationMinutes;
    private LocalDateTime estimatedArrivalTime;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String poiType; // АЗС, кафе, отель и т.д.
}
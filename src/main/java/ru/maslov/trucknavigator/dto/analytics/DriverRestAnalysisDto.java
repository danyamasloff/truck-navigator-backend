package ru.maslov.trucknavigator.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для анализа режима труда и отдыха водителя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRestAnalysisDto {
    private Long driverId;
    private String driverName;
    private Integer remainingContinuousDrivingMinutes;
    private Integer remainingDailyDrivingMinutes;
    private Integer estimatedTripDurationMinutes;
    private List<RestStopDto> requiredRestStops = new ArrayList<>();
    private Boolean isCompliant;
    private String complianceNotes;
}
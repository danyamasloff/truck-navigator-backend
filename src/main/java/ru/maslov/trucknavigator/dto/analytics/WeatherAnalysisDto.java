package ru.maslov.trucknavigator.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAnalysisDto {
    private List<WeatherPointDto> weatherPoints;
    private boolean hasPrecipitation;
    private boolean hasStrongWind;
    private boolean hasExtremeTemperature;
    private BigDecimal overallWeatherRiskScore;
}

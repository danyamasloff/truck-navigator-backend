package ru.maslov.trucknavigator.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherPointDto {
    private double latitude;
    private double longitude;
    private String pointType; // START, MIDDLE, END
    private LocalDateTime forecastTime;
    private double temperature;
    private double windSpeed;
    private String weatherCondition;
    private boolean hasPrecipitation;
    private double precipitationAmount;
    private int weatherRiskScore;
}

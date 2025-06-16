package ru.maslov.trucknavigator.dto.routing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.geolatte.geom.LineString;
import org.geolatte.geom.G2D;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для ответа с данными построенного маршрута.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponseDto {

    // Основные параметры маршрута
    private BigDecimal distance;  // в километрах
    private long duration;        // в минутах

    // Геометрия маршрута
    @Builder.Default
    private List<double[]> coordinates = new ArrayList<>();

    // Используем JsonIgnore, чтобы исключить поле из сериализации JSON
    @JsonIgnore
    private LineString<G2D> geometry;

    // Список инструкций
    @Builder.Default
    private List<Instruction> instructions = new ArrayList<>();

    // Время отправления
    private LocalDateTime departureTime;

    // Информация о рисках
    private BigDecimal weatherRiskScore;
    private BigDecimal roadQualityRiskScore;
    private BigDecimal trafficRiskScore;
    private BigDecimal overallRiskScore;

    // Экономические показатели
    private BigDecimal estimatedFuelConsumption;
    private BigDecimal estimatedFuelCost;
    private BigDecimal estimatedTollCost;
    private BigDecimal estimatedDriverCost;
    private BigDecimal estimatedTotalCost;

    @Builder.Default
    private List<RoadQualitySegment> roadQualitySegments = new ArrayList<>();
    @Builder.Default
    private List<WeatherAlertSegment> weatherAlertSegments = new ArrayList<>();
    @Builder.Default
    private List<TollSegment> tollSegments = new ArrayList<>();

    //Комплаенс-Ассистент РТО
    private boolean rtoCompliant;
    @Builder.Default
    private List<String> rtoWarnings = new ArrayList<>();

    /**
     * Инструкция для навигации.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Instruction {
        private String text;
        private BigDecimal distance;  // в километрах
        private long time;            // в минутах
        private String streetName;
        private Integer exitNumber;   // для развязок
        private Integer turnAngle;    // угол поворота в градусах
    }

    // Остальные внутренние классы не меняются
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoadQualitySegment {
        private int startIndex;       // индекс начальной точки в массиве координат
        private int endIndex;         // индекс конечной точки в массиве координат
        private BigDecimal distance;  // длина сегмента в километрах
        private String quality;       // качество дороги: EXCELLENT, GOOD, FAIR, POOR, VERY_POOR
        private String surfaceType;   // тип покрытия: ASPHALT, CONCRETE, GRAVEL, UNPAVED
        private String description;   // описание состояния дороги
        private BigDecimal riskScore; // оценка риска для этого сегмента
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherAlertSegment {
        private int startIndex;       // индекс начальной точки в массиве координат
        private int endIndex;         // индекс конечной точки в массиве координат
        private BigDecimal distance;  // длина сегмента в километрах
        private String weatherType;   // тип погоды: RAIN, SNOW, ICE, FOG, STRONG_WIND
        private String severity;      // уровень серьезности: LOW, MODERATE, HIGH, SEVERE
        private String description;   // описание погодного явления
        private BigDecimal riskScore; // оценка риска для этого сегмента
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TollSegment {
        private int startIndex;       // индекс начальной точки в массиве координат
        private int endIndex;         // индекс конечной точки в массиве координат
        private BigDecimal distance;  // длина сегмента в километрах
        private String tollName;      // название платной дороги
        private BigDecimal cost;      // стоимость проезда
        private String currency;      // валюта
    }
}

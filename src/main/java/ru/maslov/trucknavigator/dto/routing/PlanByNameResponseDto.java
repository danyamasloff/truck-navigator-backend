package ru.maslov.trucknavigator.dto.routing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для ответа от API планирования маршрута по координатам.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanByNameResponseDto {
    
    /**
     * Расстояние маршрута в километрах
     */
    private double distance;
    
    /**
     * Продолжительность маршрута в минутах
     */
    private int duration;
    
    /**
     * Координаты маршрута в формате [[lon, lat], [lon, lat], ...]
     */
    private List<List<Double>> coordinates;
} 
package ru.maslov.trucknavigator.dto.graphhopper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO, представляющий точки маршрута в ответе API GraphHopper.
 * Содержит координаты в формате [долгота, широта], когда points_encoded=false.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class PointsDto {
    private List<List<Double>> coordinates;  // When points_encoded=false (preferred)
    private String type;                     // Usually "LineString"
}
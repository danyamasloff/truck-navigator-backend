package ru.maslov.trucknavigator.dto.graphhopper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO, представляющий маршрут в ответе API GraphHopper.
 * Содержит информацию о длине маршрута, времени, координатах и инструкциях.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class PathDto {
    private Double distance;      // Distance in meters
    private Long time;            // Time in milliseconds
    private Double ascend;        // Ascend in meters
    private Double descend;       // Descend in meters
    private PointsDto points;     // Route geometry
    private List<InstructionDto> instructions;
}

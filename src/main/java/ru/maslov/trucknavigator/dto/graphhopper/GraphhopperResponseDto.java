package ru.maslov.trucknavigator.dto.graphhopper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для маппинга корневого ответа от API GraphHopper.
 * Представляет верхний уровень данных, возвращаемых сервисом.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphhopperResponseDto {
    private List<PathDto> paths; // Список маршрутов (обычно содержит только один маршрут)
}
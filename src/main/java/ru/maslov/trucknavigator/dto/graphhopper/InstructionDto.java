package ru.maslov.trucknavigator.dto.graphhopper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO, представляющий инструкцию в ответе API GraphHopper.
 * Содержит текст инструкции, расстояние, время и другие параметры навигации.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class InstructionDto {
    private String text;           // Instruction text
    private Double distance;       // Distance for this instruction in meters
    private Long time;             // Time for this instruction in milliseconds
    private Integer sign;          // Direction sign (e.g., 0=straight, 1=slight right, etc.)
    private String streetName;     // Street name
    private Integer interval;      // Interval of points (indices) for this instruction
    private Integer exit_number;   // Exit number for roundabouts
    private Double heading;        // Heading in degrees
}
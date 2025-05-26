package ru.maslov.trucknavigator.dto.assignment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoAssignmentDto {
    private Long cargoId;
    private Long routeId;
    private Long vehicleId;
    private Long driverId;
    private LocalDateTime assignmentDate;
    private String status; // ASSIGNED, IN_PROGRESS, COMPLETED
}
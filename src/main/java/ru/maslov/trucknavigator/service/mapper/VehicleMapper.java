package ru.maslov.trucknavigator.service.mapper;

import org.springframework.stereotype.Component;
import ru.maslov.trucknavigator.dto.vehicle.VehicleDetailDto;
import ru.maslov.trucknavigator.dto.vehicle.VehicleSummaryDto;
import ru.maslov.trucknavigator.entity.Vehicle;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования между сущностью Vehicle и ее DTO.
 */
@Component
public class VehicleMapper {

    /**
     * Преобразует сущность Vehicle в DTO для списка.
     */
    public VehicleSummaryDto toSummaryDto(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        return VehicleSummaryDto.builder()
                .id(vehicle.getId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .model(vehicle.getModel())
                .manufacturer(vehicle.getManufacturer())
                .productionYear(vehicle.getProductionYear())
                .heightCm(vehicle.getHeightCm())
                .widthCm(vehicle.getWidthCm())
                .lengthCm(vehicle.getLengthCm())
                .maxLoadCapacityKg(vehicle.getMaxLoadCapacityKg())
                .engineType(vehicle.getEngineType())
                .fuelConsumptionPer100km(vehicle.getFuelConsumptionPer100km())
                .currentFuelLevelLitres(vehicle.getCurrentFuelLevelLitres())
                .currentOdometerKm(vehicle.getCurrentOdometerKm())
                .build();
    }

    /**
     * Преобразует сущность Vehicle в детальное DTO.
     */
    public VehicleDetailDto toDetailDto(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        return VehicleDetailDto.builder()
                .id(vehicle.getId())
                .registrationNumber(vehicle.getRegistrationNumber())
                .model(vehicle.getModel())
                .manufacturer(vehicle.getManufacturer())
                .productionYear(vehicle.getProductionYear())
                .heightCm(vehicle.getHeightCm())
                .widthCm(vehicle.getWidthCm())
                .lengthCm(vehicle.getLengthCm())
                .emptyWeightKg(vehicle.getEmptyWeightKg())
                .maxLoadCapacityKg(vehicle.getMaxLoadCapacityKg())
                .grossWeightKg(vehicle.getGrossWeightKg())
                .engineType(vehicle.getEngineType())
                .fuelCapacityLitres(vehicle.getFuelCapacityLitres())
                .fuelConsumptionPer100km(vehicle.getFuelConsumptionPer100km())
                .emissionClass(vehicle.getEmissionClass())
                .axisConfiguration(vehicle.getAxisConfiguration())
                .axisCount(vehicle.getAxisCount())
                .hasRefrigerator(vehicle.isHasRefrigerator())
                .hasDangerousGoodsPermission(vehicle.isHasDangerousGoodsPermission())
                .hasOversizedCargoPermission(vehicle.isHasOversizedCargoPermission())
                .currentFuelLevelLitres(vehicle.getCurrentFuelLevelLitres())
                .currentOdometerKm(vehicle.getCurrentOdometerKm())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    /**
     * Преобразует список сущностей Vehicle в список DTO для списка.
     */
    public List<VehicleSummaryDto> toSummaryDtoList(List<Vehicle> vehicles) {
        if (vehicles == null) {
            return List.of();
        }

        return vehicles.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}
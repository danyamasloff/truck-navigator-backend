package ru.maslov.trucknavigator.service.mapper;

import org.springframework.stereotype.Component;
import ru.maslov.trucknavigator.dto.cargo.CargoDetailDto;
import ru.maslov.trucknavigator.dto.cargo.CargoSummaryDto;
import ru.maslov.trucknavigator.entity.Cargo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования между сущностью Cargo и ее DTO.
 */
@Component
public class CargoMapper {

    /**
     * Преобразует сущность Cargo в DTO для списка.
     */
    public CargoSummaryDto toSummaryDto(Cargo cargo) {
        if (cargo == null) {
            return null;
        }

        return CargoSummaryDto.builder()
                .id(cargo.getId())
                .name(cargo.getName())
                .description(cargo.getDescription())
                .weightKg(cargo.getWeightKg())
                .cargoType(cargo.getCargoType())
                .isFragile(cargo.isFragile())
                .isPerishable(cargo.isPerishable())
                .isDangerous(cargo.isDangerous())
                .isOversized(cargo.isOversized())
                .requiresTemperatureControl(cargo.isRequiresTemperatureControl())
                .declaredValue(cargo.getDeclaredValue())
                .currency(cargo.getCurrency())
                .build();
    }

    /**
     * Преобразует сущность Cargo в детальное DTO.
     */
    public CargoDetailDto toDetailDto(Cargo cargo) {
        if (cargo == null) {
            return null;
        }

        return CargoDetailDto.builder()
                .id(cargo.getId())
                .name(cargo.getName())
                .description(cargo.getDescription())
                .weightKg(cargo.getWeightKg())
                .volumeCubicMeters(cargo.getVolumeCubicMeters())
                .lengthCm(cargo.getLengthCm())
                .widthCm(cargo.getWidthCm())
                .heightCm(cargo.getHeightCm())
                .cargoType(cargo.getCargoType())
                .isFragile(cargo.isFragile())
                .isPerishable(cargo.isPerishable())
                .isDangerous(cargo.isDangerous())
                .dangerousGoodsClass(cargo.getDangerousGoodsClass())
                .unNumber(cargo.getUnNumber())
                .isOversized(cargo.isOversized())
                .requiresTemperatureControl(cargo.isRequiresTemperatureControl())
                .minTemperatureCelsius(cargo.getMinTemperatureCelsius())
                .maxTemperatureCelsius(cargo.getMaxTemperatureCelsius())
                .requiresCustomsClearance(cargo.isRequiresCustomsClearance())
                .declaredValue(cargo.getDeclaredValue())
                .currency(cargo.getCurrency())
                .createdAt(cargo.getCreatedAt())
                .updatedAt(cargo.getUpdatedAt())
                .build();
    }

    /**
     * Преобразует список сущностей Cargo в список DTO для списка.
     */
    public List<CargoSummaryDto> toSummaryDtoList(List<Cargo> cargos) {
        if (cargos == null) {
            return List.of();
        }

        return cargos.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}
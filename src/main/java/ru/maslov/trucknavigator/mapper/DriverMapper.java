package ru.maslov.trucknavigator.mapper;
import ru.maslov.trucknavigator.entity.DrivingStatus;

import org.springframework.stereotype.Component;
import ru.maslov.trucknavigator.dto.driver.DriverDetailDto;
import ru.maslov.trucknavigator.dto.driver.DriverSummaryDto;
import ru.maslov.trucknavigator.entity.Driver;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования между сущностью Driver и ее DTO.
 */
@Component
public class DriverMapper {

    /**
     * Преобразует сущность Driver в DTO для списка.
     */
    public DriverSummaryDto toSummaryDto(Driver driver) {
        if (driver == null) {
            return null;
        }

        return DriverSummaryDto.builder()
                .id(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .middleName(driver.getMiddleName())
                .licenseNumber(driver.getLicenseNumber())
                .phoneNumber(driver.getPhoneNumber())
                .drivingExperienceYears(driver.getDrivingExperienceYears())
                .currentDrivingStatus(driver.getCurrentDrivingStatus())
                .build();
    }

    /**
     * Преобразует сущность Driver в детальное DTO.
     */
    public DriverDetailDto toDetailDto(Driver driver) {
        if (driver == null) {
            return null;
        }

        return DriverDetailDto.builder()
                .id(driver.getId())
                .firstName(driver.getFirstName())
                .lastName(driver.getLastName())
                .middleName(driver.getMiddleName())
                .birthDate(driver.getBirthDate())
                .licenseNumber(driver.getLicenseNumber())
                .licenseIssueDate(driver.getLicenseIssueDate())
                .licenseExpiryDate(driver.getLicenseExpiryDate())
                .licenseCategories(driver.getLicenseCategories())
                .phoneNumber(driver.getPhoneNumber())
                .email(driver.getEmail())
                .drivingExperienceYears(driver.getDrivingExperienceYears())
                .hasDangerousGoodsCertificate(driver.isHasDangerousGoodsCertificate())
                .dangerousGoodsCertificateExpiry(driver.getDangerousGoodsCertificateExpiry())
                .hasInternationalTransportationPermit(driver.isHasInternationalTransportationPermit())
                .hourlyRate(driver.getHourlyRate())
                .perKilometerRate(driver.getPerKilometerRate())
                .currentDrivingStatus(driver.getCurrentDrivingStatus())
                .currentStatusStartTime(driver.getCurrentStatusStartTime())
                .dailyDrivingMinutesToday(driver.getDailyDrivingMinutesToday())
                .continuousDrivingMinutes(driver.getContinuousDrivingMinutes())
                .weeklyDrivingMinutes(driver.getWeeklyDrivingMinutes())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }

    /**
     * Преобразует список сущностей Driver в список DTO для списка.
     */
    public List<DriverSummaryDto> toSummaryDtoList(List<Driver> drivers) {
        if (drivers == null) {
            return List.of();
        }

        return drivers.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}

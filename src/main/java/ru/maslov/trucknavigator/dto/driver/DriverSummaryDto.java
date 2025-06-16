package ru.maslov.trucknavigator.dto.driver;
import ru.maslov.trucknavigator.entity.DrivingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Driver;

import java.time.LocalDate;

/**
 * DTO для отображения водителя в списках.
 * Содержит только основные данные без детальной информации.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverSummaryDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String licenseNumber;
    private String phoneNumber;
    private Integer drivingExperienceYears;
    private DrivingStatus currentDrivingStatus;

    /**
     * Возвращает полное имя водителя
     */
    public String getFullName() {
        if (middleName != null && !middleName.isEmpty()) {
            return lastName + " " + firstName + " " + middleName;
        }
        return lastName + " " + firstName;
    }

    /**
     * Проверяет, действителен ли статус водителя для рейса
     */
    public boolean isAvailableForDriving() {
        return currentDrivingStatus == DrivingStatus.AVAILABILITY ||
                currentDrivingStatus == null;
    }
}

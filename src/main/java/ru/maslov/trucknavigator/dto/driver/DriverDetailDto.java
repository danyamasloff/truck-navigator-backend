package ru.maslov.trucknavigator.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Driver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO для детального представления водителя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDetailDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate birthDate;
    private String licenseNumber;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpiryDate;
    private String licenseCategories;

    // Контактная информация
    private String phoneNumber;
    private String email;

    // Опыт и квалификация
    private Integer drivingExperienceYears;
    private boolean hasDangerousGoodsCertificate;
    private LocalDate dangerousGoodsCertificateExpiry;
    private boolean hasInternationalTransportationPermit;

    // Информация о работе и оплате
    private BigDecimal hourlyRate;
    private BigDecimal perKilometerRate;

    // Текущий статус РТО
    private Driver.DrivingStatus currentDrivingStatus;
    private LocalDateTime currentStatusStartTime;
    private Integer dailyDrivingMinutesToday;
    private Integer continuousDrivingMinutes;
    private Integer weeklyDrivingMinutes;

    // Метаданные
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Проверяет, действителен ли статус водителя для рейса
     */
    public boolean isAvailableForDriving() {
        return currentDrivingStatus == Driver.DrivingStatus.AVAILABILITY ||
                currentDrivingStatus == null;
    }

    /**
     * Проверяет, истек ли срок действия водительского удостоверения
     */
    public boolean isLicenseValid() {
        return licenseExpiryDate != null && licenseExpiryDate.isAfter(LocalDate.now());
    }
}
package ru.maslov.trucknavigator.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverQualificationDto {
    private String licenseNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String categories;
    private boolean hasDangerousGoodsCertificate;
    private LocalDate dangerousGoodsExpiryDate;
    private boolean hasInternationalPermit;
} 
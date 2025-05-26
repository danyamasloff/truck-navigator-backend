package ru.maslov.trucknavigator.dto.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverMedicalDto {
    private Long driverId;
    private LocalDate medicalCertificateExpiry;
    private boolean hasMedicalRestrictions;
    private String medicalRestrictions;
    private LocalDate nextMedicalCheckDate;
    private String bloodType;
    private boolean hasChronicConditions;
    private String chronicConditionsDescription;
    private String certificateNumber;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String medicalCertificateNumber;
    private String restrictions;
    private LocalDate medicalCertificateIssueDate;
    private LocalDate medicalCertificateExpiryDate;

    public DriverMedicalDto(
            String medicalCertificateNumber,
            LocalDate medicalCertificateIssueDate,
            LocalDate medicalCertificateExpiryDate,
            String medicalRestrictions) {
        this.medicalCertificateNumber = medicalCertificateNumber;
        this.medicalCertificateIssueDate = medicalCertificateIssueDate;
        this.medicalCertificateExpiryDate = medicalCertificateExpiryDate;
        this.medicalRestrictions = medicalRestrictions;
    }
}

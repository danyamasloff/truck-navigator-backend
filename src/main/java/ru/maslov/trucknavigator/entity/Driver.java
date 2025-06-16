package ru.maslov.trucknavigator.entity;
import ru.maslov.trucknavigator.entity.DrivingStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "drivers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;

    @Column(name = "license_issue_date")
    private LocalDate licenseIssueDate;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "license_categories")
    private String licenseCategories; // B, C, CE, D и т.д.

    // Контактная информация
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    // Опыт и квалификация
    @Column(name = "driving_experience_years")
    private Integer drivingExperienceYears;

    @Column(name = "has_dangerous_goods_certificate")
    private boolean hasDangerousGoodsCertificate;

    @Column(name = "dangerous_goods_certificate_expiry")
    private LocalDate dangerousGoodsCertificateExpiry;

    @Column(name = "has_international_transportation_permit")
    private boolean hasInternationalTransportationPermit;

    // Новые поля для расширенной информации о квалификации
    @ElementCollection
    @CollectionTable(name = "driver_adr_classes", joinColumns = @JoinColumn(name = "driver_id"))
    @Column(name = "adr_class")
    private Set<String> adrClasses = new HashSet<>(); // Классы ADR: 1, 2, 3...

    @Column(name = "has_oversized_cargo_permit")
    private boolean hasOversizedCargoPermit;

    @Column(name = "has_refrigerated_cargo_permit")
    private boolean hasRefrigeratedCargoPermit;

    // Медицинские показатели
    @Column(name = "medical_certificate_number")
    private String medicalCertificateNumber;

    @Column(name = "medical_certificate_issue_date")
    private LocalDate medicalCertificateIssueDate;

    @Column(name = "medical_certificate_expiry_date")
    private LocalDate medicalCertificateExpiryDate;

    @Column(name = "medical_restrictions")
    private String medicalRestrictions;

    @Column(name = "next_medical_check_date")
    private LocalDate nextMedicalCheckDate;

    // Режим труда и отдыха (РТО)
    @Column(name = "current_driving_status")
    @Enumerated(EnumType.STRING)
    private DrivingStatus currentDrivingStatus;

    @Column(name = "current_status_start_time")
    private LocalDateTime currentStatusStartTime;

    @Column(name = "daily_driving_minutes_today")
    private Integer dailyDrivingMinutesToday;

    @Column(name = "continuous_driving_minutes")
    private Integer continuousDrivingMinutes;

    @Column(name = "weekly_driving_minutes")
    private Integer weeklyDrivingMinutes;

    @Column(name = "two_week_driving_minutes")
    private Integer twoWeekDrivingMinutes;

    // Эффективность
    @Column(name = "avg_fuel_efficiency_percent")
    private Integer avgFuelEfficiencyPercent; // Процент от нормы расхода топлива

    @Column(name = "avg_delivery_time_efficiency_percent")
    private Integer avgDeliveryTimeEfficiencyPercent; // Процент от нормативного времени

    @Column(name = "rating")
    private BigDecimal rating; // Рейтинг (например, 1-5)

    @Column(name = "completed_routes_count")
    private Integer completedRoutesCount;

    @Column(name = "total_distance_driven_km")
    private BigDecimal totalDistanceDrivenKm;

    @Column(name = "incidents_count")
    private Integer incidentsCount;

    // Данные о рабочем времени
    @Column(name = "work_schedule_type")
    private String workScheduleType; // Тип графика (5/2, 2/2, гибкий)

    @Column(name = "weekly_work_hours")
    private Integer weeklyWorkHours;

    @Column(name = "last_rest_day")
    private LocalDate lastRestDay;

    // Информация о работе и оплате
    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "per_kilometer_rate")
    private BigDecimal perKilometerRate;

    // Знание маршрутов и регионов
    @ElementCollection
    @CollectionTable(name = "driver_known_regions", joinColumns = @JoinColumn(name = "driver_id"))
    @Column(name = "region_code")
    private Set<String> knownRegions = new HashSet<>();

    // Метаданные для аудита
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}

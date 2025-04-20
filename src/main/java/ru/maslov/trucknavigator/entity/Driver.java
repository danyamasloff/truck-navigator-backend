package ru.maslov.trucknavigator.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Сущность, представляющая водителя грузового транспортного средства.
 * Содержит информацию о водителе, его квалификации и режиме труда и отдыха.
 */
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

    // Информация о работе и оплате
    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "per_kilometer_rate")
    private BigDecimal perKilometerRate;

    // Текущий статус РТО (Режима Труда и Отдыха)
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

    /**
     * Статусы вождения согласно режиму труда и отдыха
     */
    public enum DrivingStatus {
        DRIVING,        // Вождение
        REST_BREAK,     // Короткий перерыв
        DAILY_REST,     // Ежедневный отдых
        WEEKLY_REST,    // Еженедельный отдых
        OTHER_WORK,     // Другая работа (погрузка/разгрузка и т.д.)
        AVAILABILITY    // Доступность (не вождение, но на рабочем месте)
    }
}
package ru.maslov.trucknavigator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность, представляющая промежуточную точку (waypoint) на маршруте.
 * Может использоваться для обозначения остановок, пунктов погрузки/разгрузки,
 * мест отдыха и других ключевых точек.
 */
@Entity
@Table(name = "waypoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Waypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "waypoint_type")
    @Enumerated(EnumType.STRING)
    private WaypointType waypointType;

    @Column(name = "planned_arrival_time")
    private LocalDateTime plannedArrivalTime;

    @Column(name = "planned_departure_time")
    private LocalDateTime plannedDepartureTime;

    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    @Column(name = "actual_departure_time")
    private LocalDateTime actualDepartureTime;

    @Column(name = "stay_duration_minutes")
    private Integer stayDurationMinutes;

    @Column(name = "notes")
    private String notes;

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
     * Типы промежуточных точек
     */
    public enum WaypointType {
        LOADING,            // Пункт погрузки
        UNLOADING,          // Пункт разгрузки
        REST,               // Место отдыха
        FUEL,               // Заправка
        CUSTOMS,            // Таможня
        TOLL,               // Пункт оплаты дорожного сбора
        WEIGHING,           // Пункт взвешивания
        TECHNICAL_STOP,     // Техническая остановка
        OTHER               // Другое
    }
}
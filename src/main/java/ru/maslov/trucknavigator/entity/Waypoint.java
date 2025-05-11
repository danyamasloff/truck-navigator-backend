package ru.maslov.trucknavigator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность промежуточной точки маршрута.
 */
@Entity
@Table(name = "waypoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Waypoint {

    /**
     * Типы промежуточных точек.
     */
    public enum WaypointType {
        WAYPOINT,      // Обычная промежуточная точка
        PICKUP,        // Точка погрузки
        DELIVERY,      // Точка разгрузки
        FUEL,          // Заправка
        FOOD,          // Место приема пищи
        REST,          // Место отдыха
        LODGING,       // Место ночлега
        CUSTOMS,       // Таможенный пункт
        TECHNICAL      // Техническая остановка
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Маршрут, к которому относится точка.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    /**
     * Порядковый номер точки в маршруте.
     */
    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /**
     * Название точки.
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * Адрес точки.
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * Широта.
     */
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    /**
     * Долгота.
     */
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    /**
     * Тип точки.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "waypoint_type")
    private WaypointType waypointType;

    /**
     * Запланированное время прибытия.
     */
    @Column(name = "planned_arrival_time")
    private LocalDateTime plannedArrivalTime;

    /**
     * Запланированное время отправления.
     */
    @Column(name = "planned_departure_time")
    private LocalDateTime plannedDepartureTime;

    /**
     * Фактическое время прибытия.
     */
    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    /**
     * Фактическое время отправления.
     */
    @Column(name = "actual_departure_time")
    private LocalDateTime actualDepartureTime;

    /**
     * Планируемая длительность остановки в минутах.
     */
    @Column(name = "stay_duration_minutes")
    private Integer stayDurationMinutes;

    /**
     * Дополнительная информация о точке.
     */
    @Column(name = "additional_info", length = 500)
    private String additionalInfo;

    /**
     * Время создания записи.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Время последнего обновления записи.
     */
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
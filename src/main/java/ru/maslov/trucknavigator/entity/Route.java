package ru.maslov.trucknavigator.entity;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.geolatte.geom.LineString;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность, представляющая маршрут перевозки.
 * Содержит информацию о начальной и конечной точках, промежуточных точках,
 * геометрии маршрута, времени, расстоянии и других параметрах.
 */
@Entity
@Table(name = "routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    // Связи с другими сущностями
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_id")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private Cargo cargo;

    // Начальная и конечная точки маршрута
    @Column(name = "start_address", nullable = false)
    private String startAddress;

    @Column(name = "start_lat", nullable = false)
    private Double startLat;

    @Column(name = "start_lon", nullable = false)
    private Double startLon;

    @Column(name = "end_address", nullable = false)
    private String endAddress;

    @Column(name = "end_lat", nullable = false)
    private Double endLat;

    @Column(name = "end_lon", nullable = false)
    private Double endLon;

    // Время отправления и прибытия
    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;

    @Column(name = "actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    // Промежуточные точки (waypoints)
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<Waypoint> waypoints = new ArrayList<>();

    // Геометрия маршрута (линия в формате PostGIS)
    @Column(name = "route_geometry", columnDefinition = "geometry(LineString,4326)")
    private LineString routeGeometry;

    // Параметры маршрута
    @Column(name = "distance_km", nullable = false)
    private BigDecimal distanceKm;

    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "estimated_fuel_consumption")
    private BigDecimal estimatedFuelConsumption;

    @Column(name = "actual_fuel_consumption")
    private BigDecimal actualFuelConsumption;

    // Экономические показатели
    @Column(name = "estimated_fuel_cost")
    private BigDecimal estimatedFuelCost;

    @Column(name = "estimated_toll_cost")
    private BigDecimal estimatedTollCost;

    @Column(name = "estimated_driver_cost")
    private BigDecimal estimatedDriverCost;

    @Column(name = "estimated_total_cost")
    private BigDecimal estimatedTotalCost;

    @Column(name = "actual_total_cost")
    private BigDecimal actualTotalCost;

    @Column(name = "currency")
    private String currency;

    // Анализ рисков
    @Column(name = "overall_risk_score")
    private BigDecimal overallRiskScore; // 0-100

    @Column(name = "weather_risk_score")
    private BigDecimal weatherRiskScore; // 0-100

    @Column(name = "road_quality_risk_score")
    private BigDecimal roadQualityRiskScore; // 0-100

    @Column(name = "traffic_risk_score")
    private BigDecimal trafficRiskScore; // 0-100

    @Column(name = "cargo_risk_score")
    private BigDecimal cargoRiskScore; // 0-100

    // Статус маршрута
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RouteStatus status;

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
     * Статусы маршрута
     */
    public enum RouteStatus {
        DRAFT,              // Черновик маршрута
        PLANNED,            // Запланированный маршрут
        IN_PROGRESS,        // Маршрут в процессе выполнения
        COMPLETED,          // Завершенный маршрут
        CANCELLED,          // Отмененный маршрут
        DELAYED             // Задержанный маршрут
    }
}

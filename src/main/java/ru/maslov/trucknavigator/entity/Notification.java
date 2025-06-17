package ru.maslov.trucknavigator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Сущность уведомления для отслеживания событий в системе.
 * Создается автоматически при различных операциях (создание маршрута, назначение водителя и т.д.)
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Тип уведомления
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    /**
     * ID связанной сущности (маршрут, водитель, груз и т.д.)
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Сообщение уведомления
     */
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /**
     * Дополнительная информация в формате JSON
     */
    @Column(name = "additional_data", columnDefinition = "TEXT")
    private String additionalData;

    /**
     * Статус прочтения
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /**
     * Время создания уведомления
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Время прочтения уведомления
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * Пользователь, для которого предназначено уведомление
     * В простой версии можно оставить null для общих уведомлений
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Типы уведомлений в системе
     */
    public enum NotificationType {
        ROUTE_CREATED("Создан новый маршрут"),
        ROUTE_UPDATED("Маршрут обновлен"),
        ROUTE_DELETED("Маршрут удален"),
        ROUTE_STATUS_CHANGED("Изменен статус маршрута"),
        
        DRIVER_ASSIGNED("Водитель назначен на маршрут"),
        DRIVER_CREATED("Добавлен новый водитель"),
        DRIVER_UPDATED("Данные водителя обновлены"),
        DRIVER_STATUS_CHANGED("Изменен статус водителя"),
        
        VEHICLE_CREATED("Добавлено новое транспортное средство"),
        VEHICLE_UPDATED("Данные ТС обновлены"),
        VEHICLE_FUEL_LOW("Низкий уровень топлива"),
        VEHICLE_MAINTENANCE_DUE("Требуется техобслуживание"),
        
        CARGO_CREATED("Создан новый груз"),
        CARGO_UPDATED("Данные груза обновлены"),
        CARGO_ASSIGNED("Груз назначен на маршрут"),
        
        COMPLIANCE_WARNING("Предупреждение о нарушении требований"),
        WEATHER_ALERT("Погодное предупреждение"),
        
        SYSTEM_INFO("Системная информация"),
        SYSTEM_WARNING("Системное предупреждение"),
        SYSTEM_ERROR("Системная ошибка");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
} 
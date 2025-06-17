package ru.maslov.trucknavigator.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.maslov.trucknavigator.entity.Notification;

import java.time.LocalDateTime;

/**
 * DTO для передачи данных уведомления на фронтенд
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private String type;
    private Long entityId;
    private String message;
    private String additionalData;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Long userId;

    public static NotificationDto fromEntity(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .entityId(notification.getEntityId())
                .message(notification.getMessage())
                .additionalData(notification.getAdditionalData())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .build();
    }
} 
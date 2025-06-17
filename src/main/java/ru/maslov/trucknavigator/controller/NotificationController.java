package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.dto.notification.NotificationDto;
import ru.maslov.trucknavigator.service.NotificationService;

import java.util.List;

/**
 * Контроллер для работы с уведомлениями
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Уведомления", description = "API для работы с уведомлениями системы")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Получение всех уведомлений
     */
    @GetMapping
    @Operation(summary = "Получить все уведомления", 
               description = "Возвращает список всех уведомлений, отсортированных по времени создания")
    public ResponseEntity<List<NotificationDto>> getAllNotifications(
            @Parameter(description = "Показывать только непрочитанные")
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        
        List<NotificationDto> notifications;
        if (unreadOnly) {
            notifications = notificationService.getUnreadNotifications();
        } else {
            notifications = notificationService.getAllNotifications();
        }
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Получение количества непрочитанных уведомлений
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Получить количество непрочитанных уведомлений",
               description = "Возвращает количество непрочитанных уведомлений")
    public ResponseEntity<Long> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    /**
     * Отметить уведомление как прочитанное
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "Отметить уведомление как прочитанное",
               description = "Помечает указанное уведомление как прочитанное")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "Идентификатор уведомления")
            @PathVariable Long id) {
        
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Отметить все уведомления как прочитанные
     */
    @PutMapping("/read-all")
    @Operation(summary = "Отметить все уведомления как прочитанные",
               description = "Помечает все уведомления как прочитанные")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    /**
     * Удалить уведомление
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить уведомление",
               description = "Удаляет указанное уведомление")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(description = "Идентификатор уведомления")
            @PathVariable Long id) {
        
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
} 
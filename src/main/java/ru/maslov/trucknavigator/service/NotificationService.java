package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.notification.NotificationDto;
import ru.maslov.trucknavigator.entity.Notification;
import ru.maslov.trucknavigator.entity.User;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.repository.NotificationRepository;
import ru.maslov.trucknavigator.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с уведомлениями
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Создает новое уведомление
     */
    @Transactional
    public NotificationDto createNotification(Notification.NotificationType type, Long entityId, String message) {
        return createNotification(type, entityId, message, null, null);
    }

    /**
     * Создает новое уведомление с дополнительными данными
     */
    @Transactional
    public NotificationDto createNotification(Notification.NotificationType type, Long entityId, String message, 
                                            String additionalData) {
        return createNotification(type, entityId, message, additionalData, null);
    }

    /**
     * Создает новое уведомление для конкретного пользователя
     */
    @Transactional
    public NotificationDto createNotification(Notification.NotificationType type, Long entityId, String message, 
                                            String additionalData, Long userId) {
        log.info("Creating notification: type={}, entityId={}, message={}", type, entityId, message);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User with ID {} not found, creating notification without user", userId);
            }
        }

        Notification notification = Notification.builder()
                .type(type)
                .entityId(entityId)
                .message(message)
                .additionalData(additionalData)
                .user(user)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created with ID: {}", notification.getId());

        return NotificationDto.fromEntity(notification);
    }

    /**
     * Получает все уведомления (для простой версии без аутентификации)
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Получает только непрочитанные уведомления
     */
    @Transactional(readOnly = true)
    public List<NotificationDto> getUnreadNotifications() {
        return notificationRepository.findByReadFalseOrderByCreatedAtDesc()
                .stream()
                .map(NotificationDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество непрочитанных уведомлений
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.findByReadFalseOrderByCreatedAtDesc().size();
    }

    /**
     * Отмечает уведомление как прочитанное
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        log.info("Marking notification {} as read", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification", notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Notification {} marked as read", notificationId);
        }
    }

    /**
     * Отмечает все уведомления как прочитанные
     */
    @Transactional
    public void markAllAsRead() {
        log.info("Marking all notifications as read");
        
        List<Notification> unreadNotifications = notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();
        
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(now);
        }
        
        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read", unreadNotifications.size());
    }

    // === Методы для создания уведомлений о конкретных событиях ===

    public void notifyRouteCreated(Long routeId, String routeName) {
        createNotification(
            Notification.NotificationType.ROUTE_CREATED,
            routeId,
            "Создан новый маршрут: " + routeName
        );
    }

    public void notifyRouteUpdated(Long routeId, String routeName) {
        createNotification(
            Notification.NotificationType.ROUTE_UPDATED,
            routeId,
            "Маршрут обновлен: " + routeName
        );
    }

    public void notifyRouteStatusChanged(Long routeId, String routeName, String oldStatus, String newStatus) {
        createNotification(
            Notification.NotificationType.ROUTE_STATUS_CHANGED,
            routeId,
            String.format("Статус маршрута '%s' изменен с %s на %s", routeName, oldStatus, newStatus)
        );
    }

    public void notifyDriverAssigned(Long routeId, String routeName, String driverName) {
        createNotification(
            Notification.NotificationType.DRIVER_ASSIGNED,
            routeId,
            String.format("Водитель %s назначен на маршрут '%s'", driverName, routeName)
        );
    }

    public void notifyDriverCreated(Long driverId, String driverName) {
        createNotification(
            Notification.NotificationType.DRIVER_CREATED,
            driverId,
            "Добавлен новый водитель: " + driverName
        );
    }

    public void notifyDriverUpdated(Long driverId, String driverName) {
        createNotification(
            Notification.NotificationType.DRIVER_UPDATED,
            driverId,
            "Данные водителя обновлены: " + driverName
        );
    }

    public void notifyVehicleCreated(Long vehicleId, String vehicleNumber) {
        createNotification(
            Notification.NotificationType.VEHICLE_CREATED,
            vehicleId,
            "Добавлено новое ТС: " + vehicleNumber
        );
    }

    public void notifyVehicleUpdated(Long vehicleId, String vehicleNumber) {
        createNotification(
            Notification.NotificationType.VEHICLE_UPDATED,
            vehicleId,
            "Данные ТС обновлены: " + vehicleNumber
        );
    }

    public void notifyCargoCreated(Long cargoId, String cargoName) {
        createNotification(
            Notification.NotificationType.CARGO_CREATED,
            cargoId,
            "Создан новый груз: " + cargoName
        );
    }

    public void notifyCargoUpdated(Long cargoId, String cargoName) {
        createNotification(
            Notification.NotificationType.CARGO_UPDATED,
            cargoId,
            "Данные груза обновлены: " + cargoName
        );
    }

    public void notifyCargoAssigned(Long routeId, String routeName, String cargoName) {
        createNotification(
            Notification.NotificationType.CARGO_ASSIGNED,
            routeId,
            String.format("Груз '%s' назначен на маршрут '%s'", cargoName, routeName)
        );
    }

    /**
     * Удаляет уведомление
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        log.info("Deleting notification {}", notificationId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification", notificationId));

        notificationRepository.delete(notification);
        log.info("Notification {} deleted", notificationId);
    }
} 
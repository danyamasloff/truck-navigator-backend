package ru.maslov.trucknavigator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.maslov.trucknavigator.entity.Notification;
import ru.maslov.trucknavigator.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с уведомлениями
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Находит все уведомления, отсортированные по времени создания (новые сначала)
     */
    List<Notification> findAllByOrderByCreatedAtDesc();

    /**
     * Находит уведомления для конкретного пользователя или общие (user = null)
     */
    @Query("SELECT n FROM Notification n WHERE n.user IS NULL OR n.user = :user ORDER BY n.createdAt DESC")
    List<Notification> findAllForUserOrderByCreatedAtDesc(@Param("user") User user);

    /**
     * Находит только непрочитанные уведомления
     */
    List<Notification> findByReadFalseOrderByCreatedAtDesc();

    /**
     * Находит непрочитанные уведомления для пользователя
     */
    @Query("SELECT n FROM Notification n WHERE n.read = false AND (n.user IS NULL OR n.user = :user) ORDER BY n.createdAt DESC")
    List<Notification> findUnreadForUserOrderByCreatedAtDesc(@Param("user") User user);

    /**
     * Подсчитывает количество непрочитанных уведомлений для пользователя
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.read = false AND (n.user IS NULL OR n.user = :user)")
    long countUnreadForUser(@Param("user") User user);

    /**
     * Находит уведомления определенного типа
     */
    List<Notification> findByTypeOrderByCreatedAtDesc(Notification.NotificationType type);

    /**
     * Находит уведомления для конкретной сущности
     */
    List<Notification> findByEntityIdOrderByCreatedAtDesc(Long entityId);

    /**
     * Находит последние N уведомлений
     */
    @Query("SELECT n FROM Notification n ORDER BY n.createdAt DESC")
    List<Notification> findTopNOrderByCreatedAtDesc(@Param("limit") int limit);

    /**
     * Отмечает уведомление как прочитанное
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.id = :id")
    void markAsRead(@Param("id") Long id, @Param("readAt") LocalDateTime readAt);

    /**
     * Отмечает все уведомления пользователя как прочитанные
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :readAt WHERE n.read = false AND (n.user IS NULL OR n.user = :user)")
    void markAllAsReadForUser(@Param("user") User user, @Param("readAt") LocalDateTime readAt);

    /**
     * Удаляет старые прочитанные уведомления (старше указанной даты)
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.read = true AND n.createdAt < :cutoffDate")
    void deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Находит уведомления за период
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<Notification> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 
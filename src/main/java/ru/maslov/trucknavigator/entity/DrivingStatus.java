package ru.maslov.trucknavigator.entity;
import ru.maslov.trucknavigator.entity.DrivingStatus;

/**
 * Статусы вождения согласно режиму труда и отдыха
 */
public enum DrivingStatus {
    DRIVING,        // Вождение
    REST_BREAK,     // Короткий перерыв
    DAILY_REST,     // Ежедневный отдых
    WEEKLY_REST,    // Еженедельный отдых
    OTHER_WORK,     // Другая работа (погрузка/разгрузка и т.д.)
    AVAILABILITY,   // Доступность (не вождение, но на рабочем месте)
    OFF_DUTY        // Не на работе
} 

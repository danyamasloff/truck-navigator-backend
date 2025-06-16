package ru.maslov.trucknavigator.repository;
import ru.maslov.trucknavigator.entity.DrivingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.maslov.trucknavigator.entity.Driver;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с водителями.
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    /**
     * Находит водителя по номеру лицензии.
     *
     * @param licenseNumber номер лицензии
     * @return опциональный объект с водителем
     */
    Optional<Driver> findByLicenseNumber(String licenseNumber);

    /**
     * Находит водителей по фамилии.
     *
     * @param lastName фамилия водителя
     * @return список водителей
     */
    List<Driver> findByLastName(String lastName);

    /**
     * Находит водителей по имени и фамилии.
     *
     * @param firstName имя водителя
     * @param lastName фамилия водителя
     * @return список водителей
     */
    List<Driver> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Находит водителей с истекающим сроком действия лицензии.
     *
     * @param expiryDate дата для проверки
     * @return список водителей
     */
    List<Driver> findByLicenseExpiryDateBefore(LocalDate expiryDate);

    /**
     * Находит водителей с заданным статусом вождения.
     *
     * @param status статус вождения
     * @return список водителей
     */
    List<Driver> findByCurrentDrivingStatus(DrivingStatus status);

    /**
     * Находит водителей, у которых заканчивается время непрерывного вождения.
     *
     * @param minutes пороговое значение в минутах
     * @return список водителей
     */
    @Query("SELECT d FROM Driver d WHERE d.currentDrivingStatus = 'DRIVING' AND " +
            "d.continuousDrivingMinutes >= :minutes")
    List<Driver> findDriversApproachingContinuousDrivingLimit(@Param("minutes") int minutes);
}

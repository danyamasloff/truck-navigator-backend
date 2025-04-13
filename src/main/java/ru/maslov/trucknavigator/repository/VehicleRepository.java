package ru.maslov.trucknavigator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.maslov.trucknavigator.entity.Vehicle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с транспортными средствами.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * Находит транспортное средство по регистрационному номеру.
     *
     * @param registrationNumber регистрационный номер
     * @return опциональный объект с ТС
     */
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    /**
     * Находит транспортные средства по модели.
     *
     * @param model модель ТС
     * @return список ТС
     */
    List<Vehicle> findByModel(String model);

    /**
     * Находит транспортные средства по производителю.
     *
     * @param manufacturer производитель ТС
     * @return список ТС
     */
    List<Vehicle> findByManufacturer(String manufacturer);

    /**
     * Находит транспортные средства с низким уровнем топлива.
     *
     * @param threshold пороговое значение уровня топлива
     * @return список ТС
     */
    @Query("SELECT v FROM Vehicle v WHERE v.currentFuelLevelLitres < :threshold")
    List<Vehicle> findVehiclesWithLowFuel(@Param("threshold") BigDecimal threshold);

    /**
     * Находит транспортные средства с разрешением на перевозку опасных грузов.
     *
     * @return список ТС
     */
    List<Vehicle> findByHasDangerousGoodsPermission(boolean permission);

    /**
     * Находит транспортные средства с разрешением на перевозку негабаритных грузов.
     *
     * @return список ТС
     */
    List<Vehicle> findByHasOversizedCargoPermission(boolean permission);

    /**
     * Находит транспортные средства с рефрижератором.
     *
     * @return список ТС
     */
    List<Vehicle> findByHasRefrigerator(boolean hasRefrigerator);

    /**
     * Находит транспортные средства в заданном диапазоне грузоподъемности.
     *
     * @param minCapacity минимальная грузоподъемность
     * @param maxCapacity максимальная грузоподъемность
     * @return список ТС
     */
    @Query("SELECT v FROM Vehicle v WHERE v.maxLoadCapacityKg >= :minCapacity AND v.maxLoadCapacityKg <= :maxCapacity")
    List<Vehicle> findByLoadCapacityRange(@Param("minCapacity") Integer minCapacity,
                                          @Param("maxCapacity") Integer maxCapacity);
}
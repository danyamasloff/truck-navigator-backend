package ru.maslov.trucknavigator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.maslov.trucknavigator.entity.Cargo;

import java.util.List;

/**
 * Репозиторий для работы с грузами.
 */
@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {

    /**
     * Находит грузы по типу.
     *
     * @param type тип груза
     * @return список грузов
     */
    List<Cargo> findByCargoType(Cargo.CargoType type);

    /**
     * Находит опасные грузы.
     *
     * @return список опасных грузов
     */
    List<Cargo> findByIsDangerous(boolean isDangerous);

    /**
     * Находит грузы, требующие температурного контроля.
     *
     * @return список грузов
     */
    List<Cargo> findByRequiresTemperatureControl(boolean requiresTemperatureControl);

    /**
     * Находит негабаритные грузы.
     *
     * @return список негабаритных грузов
     */
    List<Cargo> findByIsOversized(boolean isOversized);

    /**
     * Находит грузы в заданном диапазоне веса.
     *
     * @param minWeight минимальный вес
     * @param maxWeight максимальный вес
     * @return список грузов
     */
    @Query("SELECT c FROM Cargo c WHERE c.weightKg >= :minWeight AND c.weightKg <= :maxWeight")
    List<Cargo> findByWeightRange(@Param("minWeight") Integer minWeight,
                                  @Param("maxWeight") Integer maxWeight);

    /**
     * Находит опасные грузы определенного класса.
     *
     * @param dangerousGoodsClass класс опасных грузов
     * @return список грузов
     */
    List<Cargo> findByIsDangerousAndDangerousGoodsClass(boolean isDangerous, String dangerousGoodsClass);

    /**
     * Находит грузы по номеру ООН (для опасных грузов).
     *
     * @param unNumber номер ООН
     * @return список грузов
     */
    List<Cargo> findByUnNumber(String unNumber);
}

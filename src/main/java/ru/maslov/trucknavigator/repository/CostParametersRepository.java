package ru.maslov.trucknavigator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.maslov.trucknavigator.entity.CostParameters;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Репозиторий для работы с экономическими параметрами.
 */
@Repository
public interface CostParametersRepository extends JpaRepository<CostParameters, Long> {

    /**
     * Находит активный набор параметров для указанной даты и региона.
     *
     * @param date дата, на которую нужны параметры
     * @param regionCode код региона
     * @return опциональный объект с параметрами
     */
    @Query("SELECT cp FROM CostParameters cp WHERE cp.isActive = true AND cp.regionCode = :regionCode " +
            "AND cp.effectiveDate <= :date AND (cp.expiryDate IS NULL OR cp.expiryDate >= :date) " +
            "ORDER BY cp.effectiveDate DESC")
    Optional<CostParameters> findActiveByDateAndRegion(@Param("date") LocalDate date, @Param("regionCode") String regionCode);

    /**
     * Находит активный набор параметров для указанной даты.
     *
     * @param date дата, на которую нужны параметры
     * @return опциональный объект с параметрами
     */
    @Query("SELECT cp FROM CostParameters cp WHERE cp.isActive = true AND cp.regionCode IS NULL " +
            "AND cp.effectiveDate <= :date AND (cp.expiryDate IS NULL OR cp.expiryDate >= :date) " +
            "ORDER BY cp.effectiveDate DESC")
    Optional<CostParameters> findActiveByDate(@Param("date") LocalDate date);

    /**
     * Находит активный набор параметров с указанным названием для указанной даты и региона.
     *
     * @param name название набора параметров
     * @param date дата, на которую нужны параметры
     * @param regionCode код региона
     * @return опциональный объект с параметрами
     */
    @Query("SELECT cp FROM CostParameters cp WHERE cp.parameterName = :name AND cp.isActive = true " +
            "AND cp.regionCode = :regionCode AND cp.effectiveDate <= :date " +
            "AND (cp.expiryDate IS NULL OR cp.expiryDate >= :date) " +
            "ORDER BY cp.effectiveDate DESC")
    Optional<CostParameters> findActiveByNameDateAndRegion(
            @Param("name") String name, @Param("date") LocalDate date, @Param("regionCode") String regionCode);
}
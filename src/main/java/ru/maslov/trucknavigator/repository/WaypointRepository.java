package ru.maslov.trucknavigator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.maslov.trucknavigator.entity.Waypoint;

import java.util.List;

/**
 * Репозиторий для работы с промежуточными точками маршрута.
 */
@Repository
public interface WaypointRepository extends JpaRepository<Waypoint, Long> {

    /**
     * Находит все точки для указанного маршрута, отсортированные по порядковому номеру.
     *
     * @param routeId идентификатор маршрута
     * @return список точек
     */
    List<Waypoint> findByRouteIdOrderByOrderIndex(Long routeId);

    /**
     * Удаляет все точки для указанного маршрута.
     *
     * @param routeId идентификатор маршрута
     */
    @Modifying
    @Query("DELETE FROM Waypoint w WHERE w.route.id = :routeId")
    void deleteByRouteId(Long routeId);
}
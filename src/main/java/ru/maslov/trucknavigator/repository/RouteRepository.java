package ru.maslov.trucknavigator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.maslov.trucknavigator.entity.Route;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с маршрутами.
 */
@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    /**
     * Находит маршруты по идентификатору водителя.
     *
     * @param driverId идентификатор водителя
     * @return список маршрутов
     */
    List<Route> findByDriverId(Long driverId);

    /**
     * Находит маршруты по идентификатору транспортного средства.
     *
     * @param vehicleId идентификатор транспортного средства
     * @return список маршрутов
     */
    List<Route> findByVehicleId(Long vehicleId);

    /**
     * Находит маршруты по статусу.
     *
     * @param status статус маршрута
     * @return список маршрутов
     */
    List<Route> findByStatus(Route.RouteStatus status);

    /**
     * Находит маршруты, которые будут выполняться в указанном временном диапазоне.
     *
     * @param startTime начало временного диапазона
     * @param endTime конец временного диапазона
     * @return список маршрутов
     */
    @Query("SELECT r FROM Route r WHERE " +
            "(r.departureTime >= :startTime AND r.departureTime <= :endTime) OR " +
            "(r.estimatedArrivalTime >= :startTime AND r.estimatedArrivalTime <= :endTime) OR " +
            "(r.departureTime <= :startTime AND r.estimatedArrivalTime >= :endTime)")
    List<Route> findRoutesInTimeRange(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * Находит маршруты, проходящие через указанную географическую область.
     * Для этого используется PostGIS функция ST_Intersects для проверки пересечения
     * геометрии маршрута и заданной области.
     *
     * @param minLat минимальная широта
     * @param minLon минимальная долгота
     * @param maxLat максимальная широта
     * @param maxLon максимальная долгота
     * @return список маршрутов
     */
    @Query(value = "SELECT r.* FROM routes r WHERE ST_Intersects(" +
            "r.route_geometry, " +
            "ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326))",
            nativeQuery = true)
    List<Route> findRoutesInArea(@Param("minLat") double minLat,
                                 @Param("minLon") double minLon,
                                 @Param("maxLat") double maxLat,
                                 @Param("maxLon") double maxLon);
}
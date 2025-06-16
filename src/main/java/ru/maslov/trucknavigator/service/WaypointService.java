package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.routing.WaypointDto;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Waypoint;
import ru.maslov.trucknavigator.mapper.WaypointMapper;
import ru.maslov.trucknavigator.repository.WaypointRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WaypointService {
    private final WaypointRepository waypointRepository;
    private final WaypointMapper waypointMapper;

    @Transactional(readOnly = true)
    public List<WaypointDto> listWaypoints(Long routeId) {
        return waypointRepository.findByRouteIdOrderByOrderIndex(routeId).stream()
                .map(waypointMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список сущностей Waypoint для указанного маршрута.
     * Метод необходим для RouteMapper.
     */
    @Transactional(readOnly = true)
    public List<Waypoint> findByRouteId(Long routeId) {
        return waypointRepository.findByRouteIdOrderByOrderIndex(routeId);
    }

    @Transactional(readOnly = true)
    public List<Waypoint> findEntitiesByRouteId(Long routeId) {
        return waypointRepository.findByRouteIdOrderByOrderIndex(routeId);
    }

    @Transactional
    public void deleteAllForRoute(Long routeId) {
        waypointRepository.deleteByRouteId(routeId);
    }

    @Transactional
    public void createWaypointsForRoute(Route route, List<WaypointDto> waypointDtos) {
        List<Waypoint> toSave = new ArrayList<>(waypointDtos.size());
        for (int i = 0; i < waypointDtos.size(); i++) {
            WaypointDto dto = waypointDtos.get(i);
            Waypoint wp = waypointMapper.toEntity(dto, route);
            wp.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : i + 1);
            toSave.add(wp);
        }
        waypointRepository.saveAll(toSave);
    }

    @Transactional
    public void updateWaypointsForRoute(Route route, List<WaypointDto> waypointDtos) {
        deleteAllForRoute(route.getId());
        createWaypointsForRoute(route, waypointDtos);
    }
}

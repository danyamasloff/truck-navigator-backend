package ru.maslov.trucknavigator.mapper;

import org.springframework.stereotype.Component;
import ru.maslov.trucknavigator.dto.routing.WaypointDto;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Waypoint;

@Component
public class WaypointMapper {

    public WaypointDto toDto(Waypoint waypoint) {
        if (waypoint == null) {
            return null;
        }
        return WaypointDto.builder()
                .id(waypoint.getId())
                .name(waypoint.getName())
                .address(waypoint.getAddress())
                .latitude(waypoint.getLatitude())
                .longitude(waypoint.getLongitude())
                .type(waypoint.getWaypointType() != null ? waypoint.getWaypointType().name() : null)
                .orderIndex(waypoint.getOrderIndex())
                .plannedArrivalTime(waypoint.getPlannedArrivalTime())
                .plannedDepartureTime(waypoint.getPlannedDepartureTime())
                .actualArrivalTime(waypoint.getActualArrivalTime())
                .actualDepartureTime(waypoint.getActualDepartureTime())
                .stayDurationMinutes(waypoint.getStayDurationMinutes())
                .additionalInfo(waypoint.getAdditionalInfo())
                .routeId(waypoint.getRoute() != null ? waypoint.getRoute().getId() : null)
                .build();
    }

    public Waypoint toEntity(WaypointDto dto, Route route) {
        if (dto == null) {
            return null;
        }
        Waypoint waypoint = new Waypoint();
        waypoint.setId(dto.getId());
        waypoint.setName(dto.getName());
        waypoint.setAddress(dto.getAddress());
        waypoint.setLatitude(dto.getLatitude());
        waypoint.setLongitude(dto.getLongitude());
        if (dto.getType() != null) {
            try {
                waypoint.setWaypointType(Waypoint.WaypointType.valueOf(dto.getType()));
            } catch (IllegalArgumentException ignore) {
            }
        }
        waypoint.setOrderIndex(dto.getOrderIndex());
        waypoint.setPlannedArrivalTime(dto.getPlannedArrivalTime());
        waypoint.setPlannedDepartureTime(dto.getPlannedDepartureTime());
        waypoint.setActualArrivalTime(dto.getActualArrivalTime());
        waypoint.setActualDepartureTime(dto.getActualDepartureTime());
        waypoint.setStayDurationMinutes(dto.getStayDurationMinutes());
        waypoint.setAdditionalInfo(dto.getAdditionalInfo());
        waypoint.setRoute(route);
        return waypoint;
    }
}

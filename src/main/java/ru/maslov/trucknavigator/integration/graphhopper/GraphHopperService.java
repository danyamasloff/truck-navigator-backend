package ru.maslov.trucknavigator.integration.graphhopper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geolatte.geom.G2D;
import org.geolatte.geom.LineString;
import org.geolatte.geom.PositionSequenceBuilders;
import org.geolatte.geom.crs.CoordinateReferenceSystems;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.maslov.trucknavigator.dto.routing.RouteRequestDto;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.exception.RoutingException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для взаимодействия с GraphHopper API для построения и анализа маршрутов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GraphHopperService {
    private final WebClient graphHopperWebClient;
    private final ObjectMapper objectMapper;

    @Value("${graphhopper.api.key}")
    private String apiKey;

    /**
     * Строит маршрут на основе запроса с учетом параметров ТС и груза.
     */
    public RouteResponseDto calculateRoute(RouteRequestDto request, Vehicle vehicle, Cargo cargo) {
        try {
            // Учитываем параметры ТС и груза при создании body запроса
            ObjectNode requestBody = createRoutingRequestBody(request);

            log.debug("Отправка запроса в GraphHopper: {}", requestBody);

            String responseJson = graphHopperWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new RoutingException("Ошибка GraphHopper: " + error))))
                    .bodyToMono(String.class)
                    .block();

            return parseRouteResponse(responseJson);
        } catch (Exception e) {
            log.error("Ошибка при построении маршрута: ", e);
            throw new RoutingException("Не удалось построить маршрут: " + e.getMessage(), e);
        }
    }

    /**
     * Создает тело запроса для GraphHopper API.
     */
    private ObjectNode createRoutingRequestBody(RouteRequestDto request) {
        ObjectNode requestBody = objectMapper.createObjectNode();

        // Добавление точек маршрута
        ArrayNode points = requestBody.putArray("points");

        // Добавляем начальную точку
        ArrayNode startPoint = objectMapper.createArrayNode()
                .add(request.getStartLon())
                .add(request.getStartLat());
        points.add(startPoint);

        // Добавляем промежуточные точки, если они есть
        if (request.getWaypoints() != null && !request.getWaypoints().isEmpty()) {
            for (RouteRequestDto.WaypointDto waypoint : request.getWaypoints()) {
                ArrayNode point = objectMapper.createArrayNode()
                        .add(waypoint.getLongitude())
                        .add(waypoint.getLatitude());
                points.add(point);
            }
        }

        // Добавляем конечную точку
        ArrayNode endPoint = objectMapper.createArrayNode()
                .add(request.getEndLon())
                .add(request.getEndLat());
        points.add(endPoint);

        // Профиль и параметры для расчета
        String profile = determineProfile(request);
        requestBody.put("profile", profile);
        requestBody.put("calc_points", true);
        requestBody.put("instructions", true);
        requestBody.put("points_encoded", false);

        // Добавляем параметры избегания
        if (request.isAvoidTolls()) {
            requestBody.put("avoid", "toll");
        }
        if (request.isAvoidHighways()) {
            ArrayNode avoid = requestBody.has("avoid") ? 
                (ArrayNode) requestBody.get("avoid") : requestBody.putArray("avoid");
            avoid.add("motorway");
        }

        // Добавляем параметры времени отправления для учета трафика
        if (request.getDepartureTime() != null && request.isConsiderTraffic()) {
            long departureTimestamp = request.getDepartureTime()
                    .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            requestBody.put("departure_time", departureTimestamp);
        }

        log.debug("Создан запрос для GraphHopper: профиль={}, точек={}, избегание={}", 
                profile, points.size(), 
                request.isAvoidTolls() || request.isAvoidHighways() ? "да" : "нет");

        return requestBody;
    }

    /**
     * Определяет профиль маршрутизации на основе запроса
     */
    private String determineProfile(RouteRequestDto request) {
        // Проверяем, указан ли явно профиль в запросе
        if (request.getProfile() != null && !request.getProfile().isBlank()) {
            return request.getProfile();
        }

        // Определяем профиль по vehicleId или имени запроса
        if (request.getVehicleId() != null) {
            // В реальном случае здесь будет обращение к VehicleService
            // для получения типа ТС и выбора соответствующего профиля
            return "car"; // или "truck" в зависимости от типа ТС
        }

        // По умолчанию используем автомобильный профиль
        return "car";
    }

    /**
     * Парсит ответ от GraphHopper API.
     */
    private RouteResponseDto parseRouteResponse(String responseJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseJson);

            if (!rootNode.has("paths") || rootNode.get("paths").isEmpty()) {
                throw new RoutingException("Маршрут не найден в ответе GraphHopper");
            }

            JsonNode path = rootNode.get("paths").get(0);

            RouteResponseDto response = new RouteResponseDto();

            // Базовая информация о маршруте
            response.setDistance(BigDecimal.valueOf(path.get("distance").asDouble() / 1000)); // км
            response.setDuration(path.get("time").asLong() / 60000); // минуты

            // Извлечение геометрии маршрута
            extractRouteGeometry(path, response);

            // Извлечение инструкций
            extractInstructions(path, response);

            return response;

        } catch (Exception e) {
            log.error("Ошибка при разборе ответа GraphHopper: ", e);
            throw new RoutingException("Не удалось разобрать ответ маршрутизатора: " + e.getMessage(), e);
        }
    }

    /**
     * Извлекает геометрию маршрута из ответа API
     */
    private void extractRouteGeometry(JsonNode path, RouteResponseDto response) {
        List<double[]> coordinates = new ArrayList<>();

        if (path.has("points")) {
            if (path.get("points").isObject() && path.get("points").has("coordinates")) {
                // Неэнкодированные точки (когда points_encoded=false)
                JsonNode coordsArray = path.get("points").get("coordinates");
                for (JsonNode coord : coordsArray) {
                    if (coord.isArray() && coord.size() >= 2) {
                        coordinates.add(new double[]{coord.get(0).asDouble(), coord.get(1).asDouble()});
                    }
                }
            } else if (path.get("points").isTextual()) {
                // Энкодированный полилайн (когда points_encoded=true)
                String encodedPolyline = path.get("points").asText();
                coordinates = decodePolyline(encodedPolyline);
            }

            response.setCoordinates(coordinates);

            // Создаем LineString для внутреннего использования
            if (!coordinates.isEmpty()) {
                try {
                    LineString<G2D> lineString = createLineString(coordinates);
                    response.setGeometry(lineString);
                } catch (Exception e) {
                    log.warn("Не удалось создать LineString из координат: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Извлекает инструкции маршрута из ответа API
     */
    private void extractInstructions(JsonNode path, RouteResponseDto response) {
        if (path.has("instructions")) {
            List<RouteResponseDto.Instruction> instructions = new ArrayList<>();

            for (JsonNode instructionNode : path.get("instructions")) {
                RouteResponseDto.Instruction instruction = new RouteResponseDto.Instruction();
                instruction.setText(instructionNode.get("text").asText());
                instruction.setDistance(BigDecimal.valueOf(instructionNode.get("distance").asDouble() / 1000)); // км
                instruction.setTime(instructionNode.get("time").asLong() / 60000); // минуты

                if (instructionNode.has("street_name")) {
                    instruction.setStreetName(instructionNode.get("street_name").asText());
                }

                instructions.add(instruction);
            }

            response.setInstructions(instructions);
        }
    }

    /**
     * Декодирует полилинию из формата Google Polyline в массив координат.
     */
    private List<double[]> decodePolyline(String encoded) {
        List<double[]> points = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            // Декодирование широты
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int deltaLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += deltaLat;

            // Декодирование долготы
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int deltaLon = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += deltaLon;

            points.add(new double[]{lng / 1e5, lat / 1e5});
        }

        return points;
    }

    /**
     * Создает объект LineString из списка координат для хранения в PostGIS.
     */
    private LineString<G2D> createLineString(List<double[]> coordinates) {
        var positionBuilder = PositionSequenceBuilders.variableSized(G2D.class);

        for (double[] coord : coordinates) {
            positionBuilder.add(coord[0], coord[1]); // lon, lat
        }

        return new LineString<>(positionBuilder.toPositionSequence(), CoordinateReferenceSystems.WGS84);
    }
}

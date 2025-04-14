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
     * Строит маршрут на основе запроса с учетом параметров транспортного средства и груза.
     *
     * @param request запрос на построение маршрута
     * @param vehicle транспортное средство
     * @param cargo груз (может быть null)
     * @return объект с данными маршрута
     */
    public RouteResponseDto calculateRoute(RouteRequestDto request, Vehicle vehicle, Cargo cargo) {
        try {
            ObjectNode requestBody = createRoutingRequestBody(request, vehicle, cargo);

            log.debug("Отправка запроса в GraphHopper: {}", requestBody);

            String responseJson = graphHopperWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route")
                            .queryParam("key", apiKey)  // Add API key as query parameter
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
     *
     * @param request запрос на построение маршрута
     * @param vehicle транспортное средство
     * @param cargo груз (может быть null)
     * @return тело запроса в формате JSON
     */
    private ObjectNode createRoutingRequestBody(RouteRequestDto request, Vehicle vehicle, Cargo cargo) {
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

        // Используем profile="car" вместо "truck" из-за ограничений бесплатного API
        requestBody.put("profile", "car");

        // Параметры для расчета
        requestBody.put("calc_points", true);
        requestBody.put("instructions", true);
        requestBody.put("points_encoded", false);  // Запрашиваем неэнкодированные точки для простоты

        return requestBody;
    }

    /**
     * Парсит ответ от GraphHopper API.
     *
     * @param responseJson ответ в формате JSON
     * @return объект с данными маршрута
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
            response.setDistance(new BigDecimal(path.get("distance").asDouble() / 1000)); // конвертируем в км
            response.setDuration(path.get("time").asLong() / 60000); // конвертируем в минуты

            // Извлечение геометрии маршрута
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
                    coordinates = decodePolyline(encodedPolyline, false);
                }

                response.setCoordinates(coordinates);

                // Создаем LineString для внутреннего использования (но не для отправки клиенту)
                if (!coordinates.isEmpty()) {
                    try {
                        LineString<G2D> lineString = createLineString(coordinates);
                        response.setGeometry(lineString);
                    } catch (Exception e) {
                        log.warn("Не удалось создать LineString из координат: {}", e.getMessage());
                        // Продолжаем без геометрии
                    }
                }
            }

            // Извлечение инструкций
            if (path.has("instructions")) {
                List<RouteResponseDto.Instruction> instructions = new ArrayList<>();

                for (JsonNode instructionNode : path.get("instructions")) {
                    RouteResponseDto.Instruction instruction = new RouteResponseDto.Instruction();
                    instruction.setText(instructionNode.get("text").asText());
                    instruction.setDistance(new BigDecimal(instructionNode.get("distance").asDouble() / 1000)); // в км
                    instruction.setTime(instructionNode.get("time").asLong() / 60000); // в минутах

                    if (instructionNode.has("street_name")) {
                        instruction.setStreetName(instructionNode.get("street_name").asText());
                    }

                    instructions.add(instruction);
                }

                response.setInstructions(instructions);
            }

            return response;

        } catch (Exception e) {
            log.error("Ошибка при разборе ответа GraphHopper: ", e);
            throw new RoutingException("Не удалось разобрать ответ маршрутизатора: " + e.getMessage(), e);
        }
    }

    /**
     * Декодирует полилинию из формата Google Polyline в массив координат.
     *
     * @param encoded закодированная строка полилинии
     * @param is3D флаг, указывающий на наличие данных о высоте
     * @return список координат [lon, lat, (alt)]
     */
    private List<double[]> decodePolyline(String encoded, boolean is3D) {
        List<double[]> points = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0, lng = 0, ele = 0;

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

            // Если есть данные о высоте
            if (is3D && index < len) {
                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int deltaEle = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                ele += deltaEle;

                points.add(new double[]{lng / 1e5, lat / 1e5, ele / 100});
            } else {
                points.add(new double[]{lng / 1e5, lat / 1e5});
            }
        }

        return points;
    }

    /**
     * Создает объект LineString из списка координат для хранения в PostGIS.
     *
     * @param coordinates список координат [lon, lat]
     * @return объект LineString
     */
    private LineString<G2D> createLineString(List<double[]> coordinates) {
        var positionBuilder = PositionSequenceBuilders.variableSized(G2D.class);

        for (double[] coord : coordinates) {
            positionBuilder.add(coord[0], coord[1]); // lon, lat
        }

        return new LineString<>(positionBuilder.toPositionSequence(), CoordinateReferenceSystems.WGS84);
    }
}
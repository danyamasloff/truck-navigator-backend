package ru.maslov.trucknavigator.integration.graphhopper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.maslov.trucknavigator.dto.geocoding.GeoPoint;
import ru.maslov.trucknavigator.exception.GeocodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final WebClient graphHopperWebClient;
    private final ObjectMapper objectMapper;

    @Value("${graphhopper.api.key}")
    private String apiKey;

    /**
     * Converts a place name to geographic coordinates using GraphHopper Geocoding API.
     *
     * @param placeName Place name to geocode (e.g., "Moscow")
     * @return GeoPoint containing latitude and longitude
     * @throws GeocodingException if geocoding fails
     */
    public GeoPoint geocode(String placeName) {
        try {
            log.debug("Geocoding place: {}", placeName);

            String responseJson = graphHopperWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/geocode")
                            .queryParam("q", placeName)
                            .queryParam("limit", 1)
                            .queryParam("locale", "ru")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new GeocodingException("Ошибка геокодирования: " + error))))
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(responseJson);

            if (!rootNode.has("hits") || rootNode.get("hits").isEmpty()) {
                throw new GeocodingException("Не найдено результатов для места: " + placeName);
            }

            JsonNode firstHit = rootNode.get("hits").get(0);
            JsonNode point = firstHit.get("point");

            double lat = point.get("lat").asDouble();
            double lng = point.get("lng").asDouble();

            log.debug("Geocoded {} to coordinates: [{}, {}]", placeName, lat, lng);
            return new GeoPoint(lat, lng);

        } catch (Exception e) {
            log.error("Ошибка при геокодировании места {}: {}", placeName, e.getMessage());
            throw new GeocodingException("Не удалось выполнить геокодирование для места: " + placeName, e);
        }
    }
}
package ru.maslov.trucknavigator.integration.graphhopper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.maslov.trucknavigator.dto.geocoding.GeoLocationDto;
import ru.maslov.trucknavigator.dto.geocoding.GeoPoint;
import ru.maslov.trucknavigator.exception.GeocodingException;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Сервис для геокодирования и расширенного поиска POI через GraphHopper API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final WebClient graphHopperWebClient;
    private final ObjectMapper objectMapper;

    @Value("${graphhopper.api.key}")
    private String apiKey;

    private final AtomicLong locationIdGenerator = new AtomicLong(1);

    /**
     * Forward геокодирование: текст -> координаты
     */
    @Cacheable(value = "geocodeCache", key = "#placeName")
    public GeoPoint geocode(String placeName) {
        try {
            String json = graphHopperWebClient.get()
                    .uri(uri -> uri.path("/geocode")
                            .queryParam("q", placeName)
                            .queryParam("limit", 1)
                            .queryParam("locale", "ru")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(err -> Mono.error(new GeocodingException("Ошибка геокодирования: " + err))))
                    .bodyToMono(String.class)
                    .block();

            JsonNode hits = objectMapper.readTree(json).path("hits");
            if (!hits.isArray() || hits.isEmpty()) {
                throw new GeocodingException("Не найдено результатов для: " + placeName);
            }
            JsonNode pt = hits.get(0).path("point");
            return new GeoPoint(pt.path("lat").asDouble(), pt.path("lng").asDouble());
        } catch (Exception e) {
            log.error("geocode error {}: {}", placeName, e.getMessage());
            throw new GeocodingException("Не удалось выполнить геокодирование: " + e.getMessage(), e);
        }
    }

    /**
     * Reverse геокодирование: координаты -> адрес
     */
    @Cacheable(value = "reverseGeocodeCache", key = "{#lat,#lon}")
    public GeoLocationDto reverseGeocode(double lat, double lon) {
        try {
            String json = graphHopperWebClient.get()
                    .uri(uri -> uri.path("/geocode")
                            .queryParam("reverse", true)
                            .queryParam("point", lat + "," + lon)
                            .queryParam("limit", 1)
                            .queryParam("locale", "ru")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(err -> Mono.error(new GeocodingException("Ошибка обратного геокодирования: " + err))))
                    .bodyToMono(String.class)
                    .block();

            List<GeoLocationDto> list = parseLocationResults(json);
            if (list.isEmpty()) {
                throw new GeocodingException(String.format("Не найден адрес для: %f,%f", lat, lon));
            }
            return list.get(0);
        } catch (Exception e) {
            log.error("reverseGeocode error {} {}: {}", lat, lon, e.getMessage());
            throw new GeocodingException("Не удалось выполнить обратное геокодирование: " + e.getMessage(), e);
        }
    }

    /**
     * Forward поиск мест по запросу и тегу
     */
    @Cacheable(value = "searchCache", key = "{#query,#osmTag,#limit,#lat,#lon}")
    public List<GeoLocationDto> searchPlaces(String query, String osmTag, Integer limit, Double lat, Double lon) {
        try {
            int lim = (limit != null && limit > 0) ? Math.min(limit, 10) : 5;
            var uriBuilder = UriComponentsBuilder.fromPath("/geocode")
                    .queryParam("q", query)
                    .queryParam("locale", "ru")
                    .queryParam("limit", lim)
                    .queryParam("key", apiKey);
            if (osmTag != null) uriBuilder.queryParam("osm_tag", osmTag);
            if (lat != null && lon != null) uriBuilder.queryParam("point", lat + "," + lon);

            String json = graphHopperWebClient.get()
                    .uri(uriBuilder.build().toUriString())
                    .retrieve()
                    .onStatus(st -> st.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(err -> Mono.error(new GeocodingException("Ошибка поиска мест: " + err))))
                    .bodyToMono(String.class)
                    .block();

            return parseLocationResults(json);
        } catch (Exception e) {
            log.error("searchPlaces error {}: {}", query, e.getMessage());
            throw new GeocodingException("Не удалось выполнить поиск мест: " + e.getMessage(), e);
        }
    }

    /**
     * Базовый поиск POI по тегу и радиусу
     */
    @Cacheable(value = "poisCache", key = "{#lat,#lon,#osmTag,#radius}")
    public List<GeoLocationDto> findPOIsByType(double lat, double lon, String osmTag, Integer radius) {
        try {
            double km = (radius != null && radius > 0) ? radius / 1000.0 : 15.0;
            int lim = 5;
            String json = graphHopperWebClient.get()
                    .uri(uri -> uri.path("/geocode")
                            .queryParam("reverse", true)
                            .queryParam("point", lat + "," + lon)
                            .queryParam("osm_tag", osmTag)
                            .queryParam("radius", km)
                            .queryParam("limit", lim)
                            .queryParam("locale", "ru")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(st -> st.isError(), resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(err -> Mono.error(new GeocodingException("Ошибка поиска POI: " + err))))
                    .bodyToMono(String.class)
                    .block();

            return parseLocationResults(json).stream()
                    .peek(loc -> {
                        double d = calculateDistance(lat, lon, loc.getLatitude(), loc.getLongitude());
                        loc.setDescription(loc.getDescription()
                                + (loc.getDescription().isEmpty() ? "" : " • ")
                                + String.format("%.1f км", d / 1000));
                    })
                    .sorted(Comparator.comparingDouble(loc ->
                            calculateDistance(lat, lon, loc.getLatitude(), loc.getLongitude())))
                    .limit(lim)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("findPOIsByType error {} {} {}: {}", lat, lon, osmTag, e.getMessage());
            throw new GeocodingException("Не удалось найти POI: " + e.getMessage(), e);
        }
    }

    /**
     * Расширенный поиск POI: увеличивает радиус, если пусто
     */
    public List<GeoLocationDto> findPOIWithRadiusExpansion(
            double lat, double lon,
            int initialRadius, String osmTag,
            int maxRadius, double factor) {

        if (initialRadius <= 0) initialRadius = 5000;
        if (maxRadius <= initialRadius) maxRadius = initialRadius * 5;
        if (factor <= 1.0) factor = 2.0;

        int current = initialRadius;
        boolean expanded = false;
        while (current <= maxRadius) {
            try {
                List<GeoLocationDto> found = findPOIsByType(lat, lon, osmTag, current);
                if (!found.isEmpty()) {
                    if (expanded) {
                        final int ann = current;
                        found.forEach(p -> p.setDescription(p.getDescription()
                                + String.format(" • Найдено до %.1f км", ann / 1000.0)));
                    }
                    return found;
                }
            } catch (Exception e) {
                log.warn("Ошибка POI {} м: {}", current, e.getMessage());
                break;
            }
            expanded = true;
            current = (int) (current * factor);
            log.debug("Расширяем {} до {} м", osmTag, current);
        }
        log.info("{} не найдены в {} м", osmTag, maxRadius);
        return Collections.emptyList();
    }

    /**
     * Категории с расширением:
     */
    public List<GeoLocationDto> findFuelStations(double lat, double lon, Integer radius) {
        int base = radius != null && radius > 0 ? radius : 5000;
        return findPOIWithRadiusExpansion(lat, lon, base, "amenity:fuel", base * 4, 2.0);
    }
    public List<GeoLocationDto> findRestAreas(double lat, double lon, Integer radius) {
        int base = radius != null && radius > 0 ? radius : 15000;
        return findPOIWithRadiusExpansion(lat, lon, base, "highway:rest_area", base * 3, 1.8);
    }
    public List<GeoLocationDto> findFoodStops(double lat, double lon, Integer radius) {
        int base = radius != null && radius > 0 ? radius : 10000;
        var tags = Arrays.asList("amenity:cafe", "amenity:restaurant", "amenity:fast_food");
        var all = new ArrayList<GeoLocationDto>();
        for (String t : tags) {
            try {
                all.addAll(findPOIWithRadiusExpansion(lat, lon, base, t, base * 3, 2.0));
            } catch (Exception e) {
                log.warn("Ошибка {}: {}", t, e.getMessage());
            }
        }
        return all.stream()
                .collect(Collectors.toMap(
                        loc -> loc.getLatitude() + "," + loc.getLongitude(), loc -> loc, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparingDouble(loc ->
                        calculateDistance(lat, lon, loc.getLatitude(), loc.getLongitude())))
                .limit(15)
                .collect(Collectors.toList());
    }
    public List<GeoLocationDto> findParkingSpots(double lat, double lon, Integer radius) {
        int base = radius != null && radius > 0 ? radius : 10000;
        return findPOIWithRadiusExpansion(lat, lon, base, "amenity:parking", base * 3, 2.0);
    }
    public List<GeoLocationDto> findLodging(double lat, double lon, Integer radius) {
        int base = radius != null && radius > 0 ? radius : 20000;
        var tags = Arrays.asList("tourism:hotel", "tourism:motel");
        var all = new ArrayList<GeoLocationDto>();
        for (String t : tags) {
            try {
                all.addAll(findPOIWithRadiusExpansion(lat, lon, base, t, base * 3, 2.0));
            } catch (Exception e) {
                log.warn("Ошибка {}: {}", t, e.getMessage());
            }
        }
        return all.stream()
                .collect(Collectors.toMap(
                        loc -> loc.getLatitude() + "," + loc.getLongitude(), loc -> loc, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparingDouble(loc ->
                        calculateDistance(lat, lon, loc.getLatitude(), loc.getLongitude())))
                .limit(15)
                .collect(Collectors.toList());
    }
    public List<GeoLocationDto> findAtms(double lat, double lon, Integer radius) {
        int base = radius != null && radius > 0 ? radius : 5000;
        return findPOIWithRadiusExpansion(lat, lon, base, "amenity:atm", base * 3, 2.0);
    }

    /**
     * Поиск аптек с расширением радиуса при необходимости
     */
    public List<GeoLocationDto> findPharmacies(double lat, double lon, Integer radius) {
        int base = radius != null && radius > 0 ? radius : 5000;
        return findPOIWithRadiusExpansion(lat, lon, base, "amenity:pharmacy", base * 3, 2.0);
    }

    /**
     * Поиск больниц с расширением радиуса при необходимости
     */
    public List<GeoLocationDto> findHospitals(double lat, double lon, Integer radius) {
        int base = radius != null && radius > 0 ? radius : 10000;
        return findPOIWithRadiusExpansion(lat, lon, base, "amenity:hospital", base * 3, 1.8);
    }

    private List<GeoLocationDto> parseLocationResults(String json) throws Exception {
        var res = new ArrayList<GeoLocationDto>();
        JsonNode hits = objectMapper.readTree(json).path("hits");
        if (hits.isArray()) {
            for (JsonNode h : hits) {
                if (!h.has("point")) continue;
                JsonNode p = h.get("point");
                double la = p.path("lat").asDouble(), lo = p.path("lng").asDouble();
                String name = h.path("name").asText("");
                StringBuilder d = new StringBuilder();
                if (h.hasNonNull("street")) {
                    d.append(h.get("street").asText());
                    if (h.hasNonNull("housenumber")) d.append(", ").append(h.get("housenumber").asText());
                }
                if (h.hasNonNull("city")) {
                    if (d.length()>0) d.append(", ");
                    d.append(h.get("city").asText());
                }
                if (name.isEmpty()) name = d.toString();
                String type = h.hasNonNull("osm_key") && h.hasNonNull("osm_value")
                        ? ("amenity".equals(h.get("osm_key").asText())
                        ? h.get("osm_value").asText()
                        : h.get("osm_key").asText())
                        : "place";
                res.add(GeoLocationDto.builder()
                        .id(locationIdGenerator.getAndIncrement())
                        .name(name)
                        .description(d.toString())
                        .latitude(la)
                        .longitude(lo)
                        .type(type)
                        .provider("GraphHopper")
                        .build());
            }
        }
        return res;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        return 2 * R * 1000 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

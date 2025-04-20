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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Сервис для геокодирования (преобразования текстовых адресов в координаты)
 * и обратного геокодирования (определения адреса по координатам).
 * Использует GraphHopper Geocoding API на основе данных OpenStreetMap.
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
     * Простое геокодирование: преобразует текстовый адрес или название места в координаты.
     * Возвращает координаты первого найденного результата.
     *
     * @param placeName Текстовое название места или адрес
     * @return Координаты места (широта и долгота)
     * @throws GeocodingException при ошибке геокодирования
     */
    @Cacheable(value = "geocodeCache", key = "#placeName")
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

    /**
     * Расширенный поиск мест по текстовому запросу с возможностью фильтрации по типу и поиском рядом с точкой.
     *
     * @param query Поисковый запрос (название, адрес, достопримечательность)
     * @param osmTag Тег OSM для фильтрации результатов (amenity:fuel, shop:supermarket и т.д.)
     * @param limit Максимальное количество результатов (по умолчанию 5)
     * @param referenceLatitude Опорная широта для поиска рядом
     * @param referenceLongitude Опорная долгота для поиска рядом
     * @return Список DTO с информацией о найденных местах
     * @throws GeocodingException при ошибке геокодирования
     */
    @Cacheable(value = "searchPlacesCache",
            key = "{#query, #osmTag, #limit, #referenceLatitude != null ? #referenceLatitude.toString().substring(0, 6) : 'null', " +
                    "#referenceLongitude != null ? #referenceLongitude.toString().substring(0, 6) : 'null'}")
    public List<GeoLocationDto> searchPlaces(String query, String osmTag, Integer limit,
                                             Double referenceLatitude, Double referenceLongitude) {
        try {
            log.debug("Поиск мест: запрос={}, тег={}, рядом с={},{}",
                    query, osmTag, referenceLatitude, referenceLongitude);

            // Если query пустой, используем тип места как запрос
            String effectiveQuery = query;
            if (effectiveQuery == null || effectiveQuery.isBlank()) {
                if (osmTag != null && !osmTag.isBlank()) {
                    // Извлекаем тип места из OSM-тега (например, "fuel" из "amenity:fuel")
                    String[] parts = osmTag.split(":");
                    if (parts.length > 1) {
                        effectiveQuery = parts[1];
                    } else {
                        effectiveQuery = osmTag;
                    }
                } else {
                    // Если и query, и osmTag пустые, используем общий запрос
                    effectiveQuery = "place";
                }
            }

            // Построение URL с параметрами
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/geocode")
                    .queryParam("key", apiKey)
                    .queryParam("q", effectiveQuery)
                    .queryParam("locale", "ru");

            if (osmTag != null && !osmTag.isBlank()) {
                uriBuilder.queryParam("osm_tag", osmTag);
            }

            if (limit != null && limit > 0) {
                uriBuilder.queryParam("limit", Math.min(limit, 20)); // Ограничиваем максимальное значение
            } else {
                uriBuilder.queryParam("limit", 5); // По умолчанию 5 результатов
            }

            // Добавляем опорную точку, если она указана
            if (referenceLatitude != null && referenceLongitude != null) {
                uriBuilder.queryParam("point", referenceLatitude + "," + referenceLongitude);
            }

            // Выполнение запроса
            String responseJson = graphHopperWebClient.get()
                    .uri(uriBuilder.build().toUriString())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new GeocodingException("Ошибка поиска мест: " + error))))
                    .bodyToMono(String.class)
                    .block();

            // Обработка результатов
            JsonNode rootNode = objectMapper.readTree(responseJson);
            List<GeoLocationDto> results = new ArrayList<>();

            if (rootNode.has("hits") && rootNode.get("hits").isArray()) {
                JsonNode hits = rootNode.get("hits");

                for (JsonNode hit : hits) {
                    if (hit.has("point")) {
                        JsonNode point = hit.get("point");
                        double lat = point.get("lat").asDouble();
                        double lng = point.get("lng").asDouble();

                        String name = hit.has("name") ? hit.get("name").asText() : "";

                        // Формируем описание из доступных компонентов адреса
                        StringBuilder description = new StringBuilder();
                        if (hit.has("street") && !hit.get("street").isNull()) {
                            description.append(hit.get("street").asText());

                            if (hit.has("housenumber") && !hit.get("housenumber").isNull()) {
                                description.append(", ").append(hit.get("housenumber").asText());
                            }
                            description.append(", ");
                        }

                        if (hit.has("city") && !hit.get("city").isNull()) {
                            description.append(hit.get("city").asText());

                            if (hit.has("postcode") && !hit.get("postcode").isNull()) {
                                description.append(", ").append(hit.get("postcode").asText());
                            }
                        }

                        // Определяем тип места на основе OSM-тегов или переданного osmTag
                        String type = "place";
                        if (hit.has("osm_key") && hit.has("osm_value")) {
                            String osmKey = hit.get("osm_key").asText();
                            String osmValue = hit.get("osm_value").asText();

                            if ("amenity".equals(osmKey)) {
                                if ("fuel".equals(osmValue)) {
                                    type = "fuel";
                                } else if ("parking".equals(osmValue)) {
                                    type = "parking";
                                } else if ("restaurant".equals(osmValue) || "cafe".equals(osmValue)) {
                                    type = "food";
                                }
                            } else if ("highway".equals(osmKey)) {
                                type = "road";
                            } else if ("shop".equals(osmKey)) {
                                type = "shop";
                            }
                        } else if (osmTag != null) {
                            // Если тип не определен по данным от API, но указан в запросе
                            String[] parts = osmTag.split(":");
                            if (parts.length > 1) {
                                type = parts[1]; // Например, "fuel" из "amenity:fuel"
                            }
                        }

                        // Улучшаем имя, если оно пустое
                        if (name.isEmpty()) {
                            // Используем тип места как имя
                            name = type.substring(0, 1).toUpperCase() + type.substring(1);

                            // Если есть город, добавляем его
                            if (hit.has("city") && !hit.get("city").isNull()) {
                                name += " в " + hit.get("city").asText();
                            }
                        }

                        GeoLocationDto location = GeoLocationDto.builder()
                                .id(locationIdGenerator.getAndIncrement())
                                .name(name)
                                .description(description.toString())
                                .latitude(lat)
                                .longitude(lng)
                                .type(type)
                                .provider("GraphHopper")
                                .build();

                        results.add(location);
                    }
                }
            }

            log.debug("Найдено {} мест по запросу: {}", results.size(), query);
            return results;

        } catch (Exception e) {
            log.error("Ошибка при поиске мест по запросу {}: {}", query, e.getMessage());
            throw new GeocodingException("Не удалось выполнить поиск мест: " + e.getMessage(), e);
        }
    }

    /**
     * Обратное геокодирование: определяет адрес по координатам.
     *
     * @param latitude Широта
     * @param longitude Долгота
     * @return Информация о найденном месте
     * @throws GeocodingException при ошибке обратного геокодирования
     */
    @Cacheable(value = "reverseGeocodeCache",
            key = "{#latitude.toString().substring(0, 7), #longitude.toString().substring(0, 7)}")
    public GeoLocationDto reverseGeocode(double latitude, double longitude) {
        try {
            log.debug("Обратное геокодирование координат: {},{}", latitude, longitude);

            String responseJson = graphHopperWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/geocode")
                            .queryParam("reverse", true)
                            .queryParam("point", latitude + "," + longitude)
                            .queryParam("locale", "ru")
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new GeocodingException("Ошибка обратного геокодирования: " + error))))
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(responseJson);

            if (!rootNode.has("hits") || rootNode.get("hits").isEmpty()) {
                throw new GeocodingException("Не найдено адреса для координат: " + latitude + "," + longitude);
            }

            JsonNode hit = rootNode.get("hits").get(0);

            // Формируем адрес из компонентов
            StringBuilder addressBuilder = new StringBuilder();
            String name = "";

            if (hit.has("name") && !hit.get("name").isNull()) {
                name = hit.get("name").asText();
            }

            if (hit.has("street") && !hit.get("street").isNull()) {
                addressBuilder.append(hit.get("street").asText());

                if (hit.has("housenumber") && !hit.get("housenumber").isNull()) {
                    addressBuilder.append(", ").append(hit.get("housenumber").asText());
                }
                addressBuilder.append(", ");
            }

            if (hit.has("city") && !hit.get("city").isNull()) {
                addressBuilder.append(hit.get("city").asText());

                if (hit.has("postcode") && !hit.get("postcode").isNull()) {
                    addressBuilder.append(", ").append(hit.get("postcode").asText());
                }
            }

            if (addressBuilder.length() == 0 && hit.has("country") && !hit.get("country").isNull()) {
                addressBuilder.append(hit.get("country").asText());
            }

            // Если имя не определено, но есть адрес, используем его как имя
            if (name.isEmpty() && addressBuilder.length() > 0) {
                name = addressBuilder.toString();
                addressBuilder = new StringBuilder();
            }

            String type = "place";
            if (hit.has("osm_key") && hit.has("osm_value")) {
                String osmKey = hit.get("osm_key").asText();
                String osmValue = hit.get("osm_value").asText();

                if ("amenity".equals(osmKey)) {
                    type = osmValue;
                } else if ("highway".equals(osmKey)) {
                    type = "road";
                } else if ("shop".equals(osmKey)) {
                    type = "shop";
                }
            }

            GeoLocationDto result = GeoLocationDto.builder()
                    .id(locationIdGenerator.getAndIncrement())
                    .name(name)
                    .description(addressBuilder.toString())
                    .latitude(latitude)
                    .longitude(longitude)
                    .type(type)
                    .provider("GraphHopper")
                    .build();

            log.debug("Найден адрес для координат {},{}: {}", latitude, longitude, result.getName());
            return result;

        } catch (Exception e) {
            log.error("Ошибка при обратном геокодировании координат {},{}: {}", latitude, longitude, e.getMessage());
            throw new GeocodingException("Не удалось выполнить обратное геокодирование: " + e.getMessage(), e);
        }
    }

    /**
     * Находит АЗС рядом с указанными координатами в пределах заданного радиуса.
     *
     * @param latitude Широта центральной точки
     * @param longitude Долгота центральной точки
     * @param radius Радиус поиска в метрах (для фильтрации результатов)
     * @return Список найденных АЗС
     */
    @Cacheable(value = "fuelStationsCache",
            key = "{#latitude.toString().substring(0, 7), #longitude.toString().substring(0, 7), #radius}")
    public List<GeoLocationDto> findNearbyFuelStations(double latitude, double longitude, Integer radius) {
        try {
            log.debug("Поиск АЗС рядом с координатами: [{}, {}], радиус: {}м",
                    latitude, longitude, radius);

            // Устанавливаем радиус по умолчанию, если не указан
            int effectiveRadius = radius != null && radius > 0 ? radius : 5000;

            // Строим URI с акцентом на географическое ограничение
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/geocode")
                    .queryParam("key", apiKey)
                    .queryParam("point", latitude + "," + longitude)  // Географическое ограничение
                    .queryParam("osm_tag", "amenity:fuel")  // Фильтр только для АЗС
                    .queryParam("limit", 20)  // Запрашиваем больше результатов для фильтрации
                    .queryParam("locale", "ru");

            // Важно: НЕ добавляем параметр "q" (текстовый поиск)

            // Выполняем запрос
            String responseJson = graphHopperWebClient.get()
                    .uri(uriBuilder.build().toUriString())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new GeocodingException(
                                            "Ошибка поиска АЗС: " + error))))
                    .bodyToMono(String.class)
                    .block();

            // Разбираем результаты
            List<GeoLocationDto> allStations = parseLocationResults(responseJson);

            // Фильтруем по расстоянию, если указан радиус
            if (radius != null && radius > 0) {
                return filterByDistance(allStations, latitude, longitude, effectiveRadius);
            }

            return allStations;
        } catch (Exception e) {
            log.error("Ошибка при поиске АЗС рядом с [{}, {}]: {}",
                    latitude, longitude, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Разбирает ответ API GraphHopper в объекты GeoLocationDto.
     */
    private List<GeoLocationDto> parseLocationResults(String responseJson) throws Exception {
        List<GeoLocationDto> results = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(responseJson);

        if (rootNode.has("hits") && rootNode.get("hits").isArray()) {
            JsonNode hits = rootNode.get("hits");

            for (JsonNode hit : hits) {
                if (hit.has("point")) {
                    JsonNode point = hit.get("point");
                    double lat = point.get("lat").asDouble();
                    double lng = point.get("lng").asDouble();

                    // Извлекаем имя и другие детали
                    String name = hit.has("name") ? hit.get("name").asText() : "АЗС";
                    StringBuilder description = new StringBuilder();

                    // Собираем описание адреса из компонентов
                    if (hit.has("street")) {
                        description.append(hit.get("street").asText());
                        if (hit.has("housenumber")) {
                            description.append(", ").append(hit.get("housenumber").asText());
                        }
                    }

                    if (hit.has("city")) {
                        if (description.length() > 0) description.append(", ");
                        description.append(hit.get("city").asText());
                    }

                    // Извлекаем бренд из osm_tags, если доступен
                    String brand = "";
                    if (hit.has("osm_tags") && hit.get("osm_tags").has("brand")) {
                        brand = hit.get("osm_tags").get("brand").asText();
                        if (!brand.isEmpty() && !name.contains(brand)) {
                            name = brand + " " + name;
                        }
                    }

                    GeoLocationDto location = GeoLocationDto.builder()
                            .id(locationIdGenerator.getAndIncrement())
                            .name(name)
                            .description(description.toString())
                            .latitude(lat)
                            .longitude(lng)
                            .type("fuel")
                            .provider("GraphHopper")
                            .build();

                    results.add(location);
                }
            }
        }

        return results;
    }

    /**
     * Фильтрует станции по расстоянию от исходной точки.
     */
    private List<GeoLocationDto> filterByDistance(
            List<GeoLocationDto> stations, double originLat, double originLon, int radiusMeters) {

        return stations.stream()
                .filter(station -> {
                    double distance = calculateDistance(
                            originLat, originLon,
                            station.getLatitude(), station.getLongitude());
                    return distance <= radiusMeters;
                })
                .collect(Collectors.toList());
    }

    /**
     * Рассчитывает расстояние между двумя точками по формуле гаверсинусов.
     * @return Расстояние в метрах
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Радиус Земли в километрах

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // Конвертируем в метры
    }

    /**
     * Создаем специализированный метод для поиска POI вокруг точки с учетом типа места.
     * Использует комбинацию обратного геокодирования и поиска по типу.
     *
     * @param latitude Широта центра поиска
     * @param longitude Долгота центра поиска
     * @param poiType Тип POI (fuel, restaurant, etc.)
     * @param radius Примерный радиус поиска в метрах (используется для определения масштаба)
     * @return Список найденных мест
     */
    @Cacheable(value = "findPOICache",
            key = "{#latitude.toString().substring(0, 7), #longitude.toString().substring(0, 7), #poiType, #radius}")
    public List<GeoLocationDto> findPOIsByType(double latitude, double longitude, String poiType, Integer radius) {
        try {
            log.debug("Поиск POI типа {} рядом с {},{}", poiType, latitude, longitude);

            // Специальная обработка для АЗС
            if ("fuel".equalsIgnoreCase(poiType)) {
                return findNearbyFuelStations(latitude, longitude, radius);
            }

            // Преобразуем тип POI в OSM-тег
            String osmTag = convertToOsmTag(poiType);

            // Получаем информацию о локации
            GeoLocationDto location = null;
            try {
                location = reverseGeocode(latitude, longitude);
            } catch (Exception e) {
                log.warn("Не удалось выполнить обратное геокодирование, используем название по умолчанию");
                // Создаем фиктивный объект с названием города или области
                location = GeoLocationDto.builder()
                        .name("Ближайшие точки")
                        .description("Поиск вокруг указанных координат")
                        .build();
            }

            // Поисковый запрос на основе типа и местоположения
            String query = poiType;
            if (location != null && location.getName() != null) {
                // Улучшаем запрос, добавляя информацию о местоположении
                String locationName = extractCityName(location);
                if (!locationName.isEmpty()) {
                    query += " " + locationName;
                }
            }

            // Выполняем поиск с учетом типа и координат
            return searchPlaces(query, osmTag, 10, latitude, longitude);

        } catch (Exception e) {
            log.error("Ошибка при поиске POI типа {} рядом с {},{}: {}",
                    poiType, latitude, longitude, e.getMessage());

            // В случае ошибки возвращаем пустой список вместо исключения
            return new ArrayList<>();
        }
    }

    /**
     * Преобразует простой тип POI в OSM-тег
     */
    private String convertToOsmTag(String poiType) {
        switch (poiType.toLowerCase()) {
            case "fuel":
            case "gas":
            case "азс":
                return "amenity:fuel";
            case "rest":
            case "food":
            case "restaurant":
            case "кафе":
            case "ресторан":
                return "amenity:restaurant";
            case "hotel":
            case "motel":
            case "отель":
            case "гостиница":
                return "tourism:hotel";
            case "parking":
            case "парковка":
                return "amenity:parking";
            case "shop":
            case "store":
            case "магазин":
                return "shop:supermarket";
            case "rest_area":
            case "место отдыха":
                return "highway:rest_area";
            default:
                return "amenity:" + poiType;
        }
    }

    /**
     * Извлекает название города из информации о местоположении
     */
    private String extractCityName(GeoLocationDto location) {
        if (location == null) return "";

        // Если есть описание, пытаемся извлечь город из него
        if (location.getDescription() != null && !location.getDescription().isEmpty()) {
            String[] parts = location.getDescription().split(",");
            if (parts.length > 0) {
                // Вероятно, город идет после улицы
                return parts.length > 1 ? parts[1].trim() : parts[0].trim();
            }
        }

        // Если не смогли извлечь город из описания, используем имя
        if (location.getName() != null && !location.getName().isEmpty()) {
            return location.getName();
        }

        return "";
    }
}
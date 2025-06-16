package ru.maslov.trucknavigator.integration.openweather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.maslov.trucknavigator.config.WeatherConfig;
import ru.maslov.trucknavigator.dto.weather.WeatherDataDto;
import ru.maslov.trucknavigator.dto.weather.WeatherForecastDto;
import ru.maslov.trucknavigator.exception.WeatherApiException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для получения данных о погоде и прогнозов через OpenWeatherMap API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WebClient weatherWebClient;
    private final WeatherConfig weatherConfig;
    private final ObjectMapper objectMapper;

    /**
     * Получает прогноз погоды для будущей даты и времени.
     * Использует API 5-дневного прогноза с интервалом 3 часа.
     *
     * @param lat широта
     * @param lon долгота
     * @param targetTime целевое время для прогноза
     * @return объект с данными о погоде
     */
    @Cacheable(value = "weatherForecastTimeCache", key = "#lat + '-' + #lon + '-' + #targetTime.truncatedTo(T(java.time.temporal.ChronoUnit).HOURS)")
    public WeatherDataDto getForecastForTime(double lat, double lon, LocalDateTime targetTime) {
        try {
            log.debug("Запрос прогноза погоды для координат {},{} на время {}", lat, lon, targetTime);

            // Проверяем, что целевое время в будущем и не дальше 5 дней
            LocalDateTime now = LocalDateTime.now();
            if (targetTime.isBefore(now)) {
                // Для прошедшего времени возвращаем текущую погоду
                return getCurrentWeather(lat, lon);
            }

            if (targetTime.isAfter(now.plusDays(5))) {
                log.warn("Запрошено время за пределами 5-дневного прогноза, возвращаем прогноз на 5 день");
                targetTime = now.plusDays(5);
            }

            // Получаем 5-дневный прогноз
            WeatherForecastDto forecastDto = getWeatherForecast(lat, lon);

            // Находим ближайший прогноз к целевому времени
            WeatherDataDto closestForecast = null;
            long minTimeDiff = Long.MAX_VALUE;

            for (WeatherDataDto forecast : forecastDto.getForecasts()) {
                if (forecast.getForecastTime() != null) {
                    long timeDiff = Math.abs(ChronoUnit.MINUTES.between(
                            forecast.getForecastTime(), targetTime));

                    if (timeDiff < minTimeDiff) {
                        minTimeDiff = timeDiff;
                        closestForecast = forecast;
                    }
                }
            }

            if (closestForecast == null) {
                throw new WeatherApiException("Не удалось найти прогноз погоды для указанного времени");
            }

            // Рассчитываем риски для найденного прогноза
            if (closestForecast.getRiskScore() == null) {
                closestForecast.calculateRiskLevel();
                closestForecast.generateRiskDescription();
            }

            return closestForecast;

        } catch (Exception e) {
            log.error("Ошибка при получении прогноза погоды для времени {}: {}", targetTime, e.getMessage());
            throw new WeatherApiException("Ошибка получения прогноза погоды: " + e.getMessage(), e);
        }
    }

    /**
     * Получает текущую погоду для указанных координат.
     *
     * @param lat широта
     * @param lon долгота
     * @return объект с данными о текущей погоде
     */
    @Cacheable(value = "currentWeatherCache", key = "#lat + '-' + #lon")
    public WeatherDataDto getCurrentWeather(double lat, double lon) {
        try {
            String uri = UriComponentsBuilder.fromPath("/weather")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("units", "metric")
                    .queryParam("lang", "ru")
                    .queryParam("appid", weatherConfig.getKey())
                    .build()
                    .toUriString();

            String responseJson = weatherWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new WeatherApiException("Ошибка OpenWeatherMap: " + error))))
                    .bodyToMono(String.class)
                    .block();

            WeatherDataDto weatherData = parseCurrentWeatherResponse(responseJson);

            // Рассчитываем риски для погоды
            weatherData.calculateRiskLevel();
            weatherData.generateRiskDescription();

            return weatherData;
        } catch (Exception e) {
            log.error("Ошибка при получении данных о погоде: ", e);
            throw new WeatherApiException("Не удалось получить данные о погоде: " + e.getMessage(), e);
        }
    }

    /**
     * Получает 5-дневный прогноз погоды для указанных координат.
     *
     * @param lat широта
     * @param lon долгота
     * @return объект с данными о прогнозе погоды
     */
    @Cacheable(value = "weatherForecastCache", key = "#lat + '-' + #lon")
    public WeatherForecastDto getWeatherForecast(double lat, double lon) {
        try {
            String uri = UriComponentsBuilder.fromPath("/forecast")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("units", "metric")
                    .queryParam("lang", "ru")
                    .queryParam("appid", weatherConfig.getKey())
                    .build()
                    .toUriString();

            String responseJson = weatherWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(new WeatherApiException("Ошибка OpenWeatherMap: " + error))))
                    .bodyToMono(String.class)
                    .block();

            return parseWeatherForecastResponse(responseJson);
        } catch (Exception e) {
            log.error("Ошибка при получении прогноза погоды: ", e);
            throw new WeatherApiException("Не удалось получить прогноз погоды: " + e.getMessage(), e);
        }
    }

    /**
     * Анализирует риски, связанные с погодой, на маршруте в заданное время.
     *
     * @param lat широта
     * @param lon долгота
     * @param targetTime время прибытия в точку
     * @return оценка риска от 0 до 100
     */
    public int analyzeWeatherRisk(double lat, double lon, LocalDateTime targetTime) {
        try {
            WeatherDataDto weatherData = getForecastForTime(lat, lon, targetTime);
            return weatherData.getRiskScore() != null ? weatherData.getRiskScore() : 0;
        } catch (Exception e) {
            log.warn("Не удалось проанализировать погодные риски: {}", e.getMessage());
            return 0; // В случае ошибки возвращаем нулевой риск
        }
    }

    /**
     * Разбирает ответ API с текущей погодой.
     *
     * @param responseJson ответ API в формате JSON
     * @return объект с данными о текущей погоде
     */
    private WeatherDataDto parseCurrentWeatherResponse(String responseJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseJson);

            WeatherDataDto weatherData = new WeatherDataDto();

            // Основные данные о погоде
            weatherData.setTemperature(rootNode.path("main").path("temp").asDouble());
            weatherData.setFeelsLike(rootNode.path("main").path("feels_like").asDouble());
            weatherData.setHumidity(rootNode.path("main").path("humidity").asInt());
            weatherData.setPressure(rootNode.path("main").path("pressure").asInt());

            // Информация о ветре
            weatherData.setWindSpeed(rootNode.path("wind").path("speed").asDouble());
            weatherData.setWindDirection(rootNode.path("wind").path("deg").asInt());

            // Облачность и видимость
            weatherData.setCloudiness(rootNode.path("clouds").path("all").asInt());
            weatherData.setVisibility(rootNode.path("visibility").asInt());

            // Данные об осадках
            JsonNode rainNode = rootNode.path("rain");
            if (rainNode.has("1h")) {
                weatherData.setRainVolume1h(rainNode.path("1h").asDouble());
            }
            if (rainNode.has("3h")) {
                weatherData.setRainVolume3h(rainNode.path("3h").asDouble());
            }

            JsonNode snowNode = rootNode.path("snow");
            if (snowNode.has("1h")) {
                weatherData.setSnowVolume1h(snowNode.path("1h").asDouble());
            }
            if (snowNode.has("3h")) {
                weatherData.setSnowVolume3h(snowNode.path("3h").asDouble());
            }

            // Описание погоды
            JsonNode weatherArray = rootNode.path("weather");
            if (weatherArray.isArray() && weatherArray.size() > 0) {
                JsonNode weather = weatherArray.get(0);
                weatherData.setWeatherId(weather.path("id").asInt());
                weatherData.setWeatherMain(weather.path("main").asText());
                weatherData.setWeatherDescription(weather.path("description").asText());
                weatherData.setWeatherIcon(weather.path("icon").asText());
            }

            // Время восхода и заката
            long sunriseTimestamp = rootNode.path("sys").path("sunrise").asLong();
            long sunsetTimestamp = rootNode.path("sys").path("sunset").asLong();
            weatherData.setSunrise(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(sunriseTimestamp), ZoneId.systemDefault()));
            weatherData.setSunset(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(sunsetTimestamp), ZoneId.systemDefault()));

            // Название города
            weatherData.setCityName(rootNode.path("name").asText());

            return weatherData;

        } catch (Exception e) {
            log.error("Ошибка при разборе ответа о текущей погоде: ", e);
            throw new WeatherApiException("Не удалось разобрать данные о погоде: " + e.getMessage(), e);
        }
    }

    /**
     * Разбирает ответ API с прогнозом погоды.
     *
     * @param responseJson ответ API в формате JSON
     * @return объект с данными о прогнозе погоды
     */
    private WeatherForecastDto parseWeatherForecastResponse(String responseJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseJson);

            WeatherForecastDto forecast = new WeatherForecastDto();

            // Информация о городе
            JsonNode cityNode = rootNode.path("city");
            forecast.setCityName(cityNode.path("name").asText());
            forecast.setCityCountry(cityNode.path("country").asText());

            // Список прогнозов
            List<WeatherDataDto> forecasts = new ArrayList<>();
            JsonNode listNode = rootNode.path("list");

            if (listNode.isArray()) {
                for (JsonNode forecastNode : listNode) {
                    WeatherDataDto weatherData = new WeatherDataDto();

                    // Время прогноза
                    long timestamp = forecastNode.path("dt").asLong();
                    weatherData.setForecastTime(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(timestamp), ZoneId.systemDefault()));

                    // Основные данные о погоде
                    weatherData.setTemperature(forecastNode.path("main").path("temp").asDouble());
                    weatherData.setFeelsLike(forecastNode.path("main").path("feels_like").asDouble());
                    weatherData.setHumidity(forecastNode.path("main").path("humidity").asInt());
                    weatherData.setPressure(forecastNode.path("main").path("pressure").asInt());

                    // Информация о ветре
                    weatherData.setWindSpeed(forecastNode.path("wind").path("speed").asDouble());
                    weatherData.setWindDirection(forecastNode.path("wind").path("deg").asInt());

                    // Облачность
                    weatherData.setCloudiness(forecastNode.path("clouds").path("all").asInt());

                    // Данные об осадках
                    JsonNode rainNode = forecastNode.path("rain");
                    if (rainNode.has("3h")) {
                        weatherData.setRainVolume3h(rainNode.path("3h").asDouble());
                    }

                    JsonNode snowNode = forecastNode.path("snow");
                    if (snowNode.has("3h")) {
                        weatherData.setSnowVolume3h(snowNode.path("3h").asDouble());
                    }

                    // Описание погоды
                    JsonNode weatherArray = forecastNode.path("weather");
                    if (weatherArray.isArray() && weatherArray.size() > 0) {
                        JsonNode weather = weatherArray.get(0);
                        weatherData.setWeatherId(weather.path("id").asInt());
                        weatherData.setWeatherMain(weather.path("main").asText());
                        weatherData.setWeatherDescription(weather.path("description").asText());
                        weatherData.setWeatherIcon(weather.path("icon").asText());
                    }

                    // Видимость
                    if (forecastNode.has("visibility")) {
                        weatherData.setVisibility(forecastNode.path("visibility").asInt());
                    }

                    forecasts.add(weatherData);
                }
            }

            forecast.setForecasts(forecasts);

            return forecast;

        } catch (Exception e) {
            log.error("Ошибка при разборе ответа с прогнозом погоды: ", e);
            throw new WeatherApiException("Не удалось разобрать прогноз погоды: " + e.getMessage(), e);
        }
    }
}

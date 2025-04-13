package ru.maslov.trucknavigator.integration.fuelprice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.maslov.trucknavigator.dto.fuel.FuelPriceDto;
import ru.maslov.trucknavigator.dto.fuel.FuelPriceResponse;
import ru.maslov.trucknavigator.exception.ExternalServiceException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

/**
 * Сервис для получения цен на топливо.
 * В реальном проекте здесь будет интеграция с API цен на топливо.
 * Для тестовой реализации генерируем случайные цены.
 */
@Service
@Slf4j
public class FuelPriceService {

    // В тестовой реализации не используется, но будет нужно при реальной интеграции
    @Value("${fuel.price.api.url:https://api.fuelprice.example.ru}")
    private String apiUrl;

    @Value("${fuel.price.api.key:test_key}")
    private String apiKey;

    private final WebClient fuelPriceWebClient;

    // Базовые цены для разных типов топлива (для тестовых данных)
    private static final BigDecimal BASE_DIESEL_PRICE = new BigDecimal("65.50");
    private static final BigDecimal BASE_PETROL_92_PRICE = new BigDecimal("53.20");
    private static final BigDecimal BASE_PETROL_95_PRICE = new BigDecimal("57.80");
    private static final BigDecimal BASE_PETROL_98_PRICE = new BigDecimal("63.40");

    private final Random random = new Random();

    public FuelPriceService(@Qualifier("fuelPriceWebClient") WebClient fuelPriceWebClient) {
        this.fuelPriceWebClient = fuelPriceWebClient;
    }

    /**
     * Получает текущую цену на топливо для заданных координат.
     * В тестовой реализации генерирует случайные цены около базовых значений.
     *
     * @param lat широта
     * @param lon долгота
     * @param fuelType тип топлива (DIESEL, PETROL_92, PETROL_95, PETROL_98)
     * @return объект с информацией о цене на топливо
     */
    @Cacheable(value = "fuelPricesCache", key = "{#lat, #lon, #fuelType}")
    public FuelPriceDto getFuelPrice(double lat, double lon, String fuelType) {
        log.debug("Запрос цены на топливо: тип={}, координаты={},{}", fuelType, lat, lon);

        try {
            // ВАЖНО: В ТЕСТОВОЙ РЕАЛИЗАЦИИ ИСПОЛЬЗУЕМ ЗАГЛУШКУ
            // В реальном проекте здесь будет обращение к API

            // Симулируем задержку API
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Заглушка: генерируем случайную цену около базовой
            BigDecimal basePrice = getBasePriceForFuelType(fuelType);

            // Случайное отклонение ±5%
            double variation = (random.nextDouble() * 10) - 5;
            BigDecimal price = basePrice.multiply(BigDecimal.ONE.add(
                    BigDecimal.valueOf(variation).divide(BigDecimal.valueOf(100))));

            // Округляем до 2 знаков после запятой
            price = price.setScale(2, BigDecimal.ROUND_HALF_UP);

            return FuelPriceDto.builder()
                    .fuelType(fuelType)
                    .price(price)
                    .currency("RUB")
                    .latitude(lat)
                    .longitude(lon)
                    .stationName("АЗС SimulatedOil")
                    .updateTime(System.currentTimeMillis())
                    .build();

            /* Реальная реализация будет выглядеть примерно так:

            String uri = UriComponentsBuilder.fromPath("/api/fuel-prices")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("type", fuelType)
                    .queryParam("apiKey", apiKey)
                    .build()
                    .toUriString();

            FuelPriceResponse response = fuelPriceWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(error -> Mono.error(
                                            new ExternalServiceException("FuelPrice API", error))))
                    .bodyToMono(FuelPriceResponse.class)
                    .block();

            if (response != null && response.isSuccess() && !response.getData().isEmpty()) {
                return response.getData().get(0);
            } else {
                throw new ExternalServiceException("FuelPrice API", "Нет данных о ценах на топливо");
            }
            */

        } catch (Exception e) {
            log.error("Ошибка при получении цены на топливо: {}", e.getMessage());
            throw new ExternalServiceException("FuelPrice API",
                    "Не удалось получить цену на топливо: " + e.getMessage(), e);
        }
    }

    /**
     * Получает цены на топливо для маршрута.
     * Возвращает цены на топливо в начальной, средней и конечной точках маршрута.
     *
     * @param startLat широта начальной точки
     * @param startLon долгота начальной точки
     * @param endLat широта конечной точки
     * @param endLon долгота конечной точки
     * @param fuelType тип топлива
     * @return объект с информацией о ценах на топливо
     */
    @Cacheable(value = "routeFuelPricesCache",
            key = "{#startLat, #startLon, #endLat, #endLon, #fuelType}")
    public FuelPriceResponse getRouteFuelPrices(double startLat, double startLon,
                                                double endLat, double endLon,
                                                String fuelType) {
        log.debug("Запрос цен на топливо для маршрута: тип={}, маршрут=({},{})->({},{})",
                fuelType, startLat, startLon, endLat, endLon);

        try {
            // Получаем цены в начальной точке
            FuelPriceDto startPrice = getFuelPrice(startLat, startLon, fuelType);

            // Получаем цены в средней точке маршрута
            double midLat = (startLat + endLat) / 2;
            double midLon = (startLon + endLon) / 2;
            FuelPriceDto midPrice = getFuelPrice(midLat, midLon, fuelType);

            // Получаем цены в конечной точке
            FuelPriceDto endPrice = getFuelPrice(endLat, endLon, fuelType);

            // Формируем и возвращаем ответ
            FuelPriceResponse response = new FuelPriceResponse();
            response.setSuccess(true);
            response.setData(List.of(startPrice, midPrice, endPrice));

            // Находим минимальную цену
            BigDecimal minPrice = response.getData().stream()
                    .map(FuelPriceDto::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            response.setMinPrice(minPrice);

            // Находим среднюю цену
            BigDecimal avgPrice = response.getData().stream()
                    .map(FuelPriceDto::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(response.getData().size()), 2, BigDecimal.ROUND_HALF_UP);
            response.setAveragePrice(avgPrice);

            return response;

        } catch (Exception e) {
            log.error("Ошибка при получении цен на топливо для маршрута: {}", e.getMessage());
            throw new ExternalServiceException("FuelPrice API",
                    "Не удалось получить цены на топливо для маршрута: " + e.getMessage(), e);
        }
    }

    /**
     * Возвращает базовую цену для заданного типа топлива.
     *
     * @param fuelType тип топлива
     * @return базовая цена
     */
    private BigDecimal getBasePriceForFuelType(String fuelType) {
        if (fuelType == null) {
            return BASE_DIESEL_PRICE;
        }

        switch (fuelType.toUpperCase()) {
            case "DIESEL":
                return BASE_DIESEL_PRICE;
            case "PETROL_92":
                return BASE_PETROL_92_PRICE;
            case "PETROL_95":
                return BASE_PETROL_95_PRICE;
            case "PETROL_98":
                return BASE_PETROL_98_PRICE;
            default:
                return BASE_DIESEL_PRICE;
        }
    }
}
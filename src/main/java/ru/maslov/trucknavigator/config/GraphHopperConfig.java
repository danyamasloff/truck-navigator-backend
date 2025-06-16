package ru.maslov.trucknavigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация для интеграции с API GraphHopper Directions.
 * Содержит настройки URL, ключа API и создает WebClient для запросов.
 */
@Configuration
@ConfigurationProperties(prefix = "graphhopper.api")
@Data
public class GraphHopperConfig {

    private String url;  // Базовый URL API (например, https://graphhopper.com/api/1)
    private String key;  // Ключ API для авторизации запросов

    /**
     * Создает WebClient для взаимодействия с API GraphHopper.
     *
     * @return настроенный WebClient
     */
    @Bean
    public WebClient graphHopperWebClient() {
        return WebClient.builder()
                .baseUrl(url)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

package ru.maslov.trucknavigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация для интеграции с API погоды (OpenWeatherMap).
 * Содержит настройки URL, ключа API и других параметров.
 */
@Configuration
@ConfigurationProperties(prefix = "weather.api")
@Data
public class WeatherConfig {

    private String url;
    private String key;

    /**
     * Создает WebClient для взаимодействия с API погоды.
     *
     * @return настроенный WebClient
     */
    @Bean
    public WebClient weatherWebClient() {
        return WebClient.builder()
                .baseUrl(url)
                .build();
    }
}

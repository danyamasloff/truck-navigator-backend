package ru.maslov.trucknavigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация для интеграции с GraphHopper API.
 * Содержит настройки URL, ключа API и другие параметры.
 */
@Configuration
@ConfigurationProperties(prefix = "routing.engine")
@Data
public class GraphHopperConfig {

    private String url;
    private String apiKey;

    /**
     * Создает WebClient для взаимодействия с GraphHopper API.
     *
     * @return настроенный WebClient
     */
    @Bean
    public WebClient graphHopperWebClient() {
        return WebClient.builder()
                .baseUrl(url)
                .build();
    }
}
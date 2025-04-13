package ru.maslov.trucknavigator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация для интеграции с API цен на топливо.
 */
@Configuration
@ConfigurationProperties(prefix = "fuel.price.api")
@Data
public class FuelPriceConfig {

    private String url;
    private String key;

    /**
     * Создает WebClient для взаимодействия с API цен на топливо.
     *
     * @return настроенный WebClient
     */
    @Bean
    public WebClient fuelPriceWebClient() {
        return WebClient.builder()
                .baseUrl(url)
                .build();
    }
}
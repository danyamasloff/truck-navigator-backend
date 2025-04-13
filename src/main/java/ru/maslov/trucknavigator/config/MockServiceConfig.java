package ru.maslov.trucknavigator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация мок-сервисов для режима разработки и тестирования.
 * Создает заглушки для внешних API.
 */
@Configuration
@Profile("dev")
public class MockServiceConfig {

    /**
     * Создает заглушку WebClient, которая не выполняет реальных запросов.
     * Используется, если в приложении нет доступа к реальным внешним API.
     *
     * @return WebClient с заглушками для всех методов
     */
    @Bean
    public WebClient mockWebClient() {
        return WebClient.builder().build();
    }
}
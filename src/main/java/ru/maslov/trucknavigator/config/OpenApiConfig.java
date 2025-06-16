package ru.maslov.trucknavigator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация OpenAPI/Swagger для документации API.
 * Добавляет поддержку JWT аутентификации в Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${app.openapi.prod-url:https://api.truck-navigator.ru}")
    private String prodUrl;

    /**
     * Настраивает и создает объект OpenAPI с информацией о API и схемой безопасности JWT.
     *
     * @return объект OpenAPI
     */
    @Bean
    public OpenAPI truckNavigatorOpenAPI() {
        // Настройка серверов API
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Сервер разработки");

        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Продакшн сервер");

        // Контактная информация
        Contact contact = new Contact();
        contact.setEmail("info@truck-navigator.ru");
        contact.setName("Truck Navigator Team");
        contact.setUrl("https://github.com/iGh0stnat/truck-navigator-backend");

        // Лицензия
        License mitLicense = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        // Основная информация об API
        Info info = new Info()
                .title("Truck Navigator API")
                .version("1.0.0")
                .contact(contact)
                .description("API системы аналитической навигации для грузоперевозок")
                .license(mitLicense);

        // Настройка JWT авторизации
        SecurityScheme jwtScheme = new SecurityScheme()
                .name("bearerAuth")
                .description("JWT авторизация. Введите токен в формате: Bearer {token}")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        // Глобальное требование безопасности (применится ко всем операциям)
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        // Создание и настройка объекта OpenAPI
        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .components(new Components().addSecuritySchemes("bearerAuth", jwtScheme))
                .addSecurityItem(securityRequirement);
    }
}

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
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.dev-url}")
    private String devUrl;

    @Value("${app.openapi.prod-url}")
    private String prodUrl;

    /**
     * Настраивает и создает объект OpenAPI с информацией о API.
     *
     * @return объект OpenAPI
     */
    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Сервер разработки");

        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Продакшн сервер");

        Contact contact = new Contact();
        contact.setEmail("maslov@example.com");
        contact.setName("Максим Маслов");
        contact.setUrl("https://github.com/iGh0stnat/truck-navigator-backend");

        License mitLicense = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("Truck Navigator API")
                .version("1.0.0")
                .contact(contact)
                .description("API системы аналитической навигации для грузоперевозок")
                .license(mitLicense);

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()))
                .info(info)
                .servers(List.of(devServer, prodServer));
    }

    /**
     * Создает схему безопасности для JWT токена.
     *
     * @return схема безопасности
     */
    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }
}
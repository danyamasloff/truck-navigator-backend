package ru.maslov.trucknavigator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Основной класс приложения Truck Navigator.
 * Запускает Spring Boot приложение и активирует необходимые функции.
 */
@SpringBootApplication
@EnableCaching        // Активирует механизм кэширования для @Cacheable
@EnableScheduling     // Активирует планировщик задач для @Scheduled
@EnableAsync          // Активирует асинхронное выполнение для @Async
public class TruckNavigatorBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TruckNavigatorBackendApplication.class, args);
    }

}
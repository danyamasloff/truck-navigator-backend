package ru.maslov.trucknavigator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Конфигурация для PasswordEncoder.
 * Создана отдельно от SecurityConfig для избежания циклических зависимостей.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Настраивает кодировщик паролей.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 
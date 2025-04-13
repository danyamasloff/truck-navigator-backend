package ru.maslov.trucknavigator.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO с ответом, содержащим JWT токен и информацию о пользователе.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDto {

    private String token;
    private String refreshToken;
    private String username;
    private long expiresIn; // Время жизни токена в миллисекундах
}
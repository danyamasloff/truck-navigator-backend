/*
package ru.maslov.trucknavigator.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

*/
/**
 * Сервис для работы с blacklist токенов.
 * Использует Redis для хранения недействительных токенов до их естественного истечения.
 *//*

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider tokenProvider;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    */
/**
     * Добавляет токен в черный список до его естественного истечения.
     *
     * @param token JWT-токен
     * @return true если токен успешно добавлен, false если произошла ошибка
     *//*

    public boolean addToBlacklist(String token) {
        try {
            // Извлекаем имя пользователя и время истечения токена
            String username = tokenProvider.getUsernameFromToken(token);
            Date expiryDate = tokenProvider.getExpirationDateFromToken(token);

            // Если токен уже истек, нет смысла добавлять его в blacklist
            if (expiryDate == null || expiryDate.before(new Date())) {
                log.debug("Токен уже истек, не добавляем в blacklist");
                return true;
            }

            // Определяем, сколько времени осталось до истечения токена
            long ttlMillis = expiryDate.getTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) {
                return true; // Токен уже истек
            }

            // Создаем уникальный ключ для токена в Redis
            String blacklistKey = BLACKLIST_PREFIX + username + ":" +
                    token.substring(token.length() - 12); // последние 12 символов для уникальности

            // Добавляем токен в Redis с временем жизни до его истечения
            redisTemplate.opsForValue().set(blacklistKey, "BLACKLISTED");
            redisTemplate.expire(blacklistKey, ttlMillis, TimeUnit.MILLISECONDS);

            log.info("Токен пользователя {} добавлен в blacklist", username);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при добавлении токена в blacklist: {}", e.getMessage());
            return false;
        }
    }

    */
/**
     * Проверяет, находится ли токен в черном списке.
     *
     * @param token JWT-токен
     * @return true если токен в черном списке, false если токен действителен
     *//*

    public boolean isBlacklisted(String token) {
        try {
            String username = tokenProvider.getUsernameFromToken(token);
            String blacklistKey = BLACKLIST_PREFIX + username + ":" +
                    token.substring(token.length() - 12);

            Boolean exists = redisTemplate.hasKey(blacklistKey);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Ошибка при проверке токена в blacklist: {}", e.getMessage());
            return false;
        }
    }

    */
/**
     * Добавляет все refresh и access токены пользователя в черный список.
     * Используется при сбросе пароля или блокировке аккаунта.
     *
     * @param username имя пользователя
     * @return количество инвалидированных токенов
     *//*

    public int invalidateAllUserTokens(String username) {
        try {
            // Находим все токены пользователя по шаблону
            String pattern = BLACKLIST_PREFIX + username + ":*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return 0;
            }

            // Добавляем специальный ключ для запрета всех токенов пользователя
            String userBlacklistKey = BLACKLIST_PREFIX + username + ":ALL";
            redisTemplate.opsForValue().set(userBlacklistKey, "BLACKLISTED");

            // Устанавливаем срок жизни 30 дней (или другой срок действия refresh токенов)
            redisTemplate.expire(userBlacklistKey, 30, TimeUnit.DAYS);

            log.info("Все токены пользователя {} инвалидированы", username);
            return 1 + keys.size();
        } catch (Exception e) {
            log.error("Ошибка при инвалидации всех токенов пользователя {}: {}",
                    username, e.getMessage());
            return 0;
        }
    }
}*/

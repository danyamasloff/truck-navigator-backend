package ru.maslov.trucknavigator.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Утилитный класс для работы с JWT (создание, валидация и извлечение данных из токенов).
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final SecretKey signingKey;

    /**
     * Инициализирует провайдер JWT-токенов.
     * Создает безопасный ключ для подписи токенов.
     */
    public JwtTokenProvider(@Value("${app.jwt.secret}") String jwtSecret,
                            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs,
                            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;

        // Инициализируем ключ с помощью метода
        this.signingKey = createSigningKey(jwtSecret);
    }

    /**
     * Создает безопасный ключ для подписи JWT-токенов.
     */
    private SecretKey createSigningKey(String secret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);
            // Тестируем ключ с HS512
            Jwts.builder().signWith(key, SignatureAlgorithm.HS512).compact();
            log.info("Используется настроенный JWT ключ");
            return key;
        } catch (WeakKeyException e) {
            log.warn("Указанный JWT ключ недостаточно длинный для алгоритма HS512. " +
                    "Создан новый криптографически стойкий ключ.");
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }
    }

    /**
     * Генерирует JWT токен на основе аутентификации пользователя.
     *
     * @param authentication данные аутентификации
     * @return JWT токен
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername());
    }

    /**
     * Генерирует JWT токен для указанного имени пользователя.
     *
     * @param username имя пользователя
     * @return JWT токен
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Генерирует токен обновления для указанного имени пользователя.
     *
     * @param username имя пользователя
     * @return токен обновления
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Извлекает имя пользователя из JWT токена.
     *
     * @param token JWT токен
     * @return имя пользователя
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Извлекает дату истечения из JWT токена.
     *
     * @param token JWT токен
     * @return дата истечения
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * Извлекает конкретный запрос из JWT токена.
     *
     * @param token JWT токен
     * @param claimsResolver функция для извлечения запроса
     * @param <T> тип возвращаемого значения
     * @return значение запроса
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Извлекает все запросы из JWT токена.
     *
     * @param token JWT токен
     * @return все запросы
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Проверяет, истек ли срок действия токена.
     *
     * @param token JWT токен
     * @return true, если токен истек
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Проверяет валидность JWT токена.
     *
     * @param token JWT токен
     * @param userDetails данные пользователя
     * @return true, если токен валидный
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Ошибка валидации JWT токена: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет валидность JWT токена.
     *
     * @param token JWT токен
     * @return true, если токен валидный
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            log.error("Недействительная подпись JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Недействительный JWT токен: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Истек срок действия JWT токена: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Неподдерживаемый JWT токен: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims строка пуста: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Извлекает JWT токен из HTTP запроса.
     *
     * @param request HTTP запрос
     * @return JWT токен или null, если токен не найден
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

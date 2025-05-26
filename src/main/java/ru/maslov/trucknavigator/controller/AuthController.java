package ru.maslov.trucknavigator.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.maslov.trucknavigator.dto.auth.JwtResponseDto;
import ru.maslov.trucknavigator.dto.auth.LoginRequestDto;
import ru.maslov.trucknavigator.dto.auth.MessageResponse;
import ru.maslov.trucknavigator.dto.auth.RefreshTokenRequest;
import ru.maslov.trucknavigator.dto.auth.RegistrationRequestDto;
import ru.maslov.trucknavigator.entity.User;
import ru.maslov.trucknavigator.repository.UserRepository;
import ru.maslov.trucknavigator.security.JwtTokenProvider;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDto> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest) {
        log.info("Попытка аутентификации пользователя: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(loginRequest.getUsername());

        log.info("Пользователь {} успешно аутентифицирован", loginRequest.getUsername());

        return ResponseEntity.ok(new JwtResponseDto(
                jwt,
                refreshToken,
                loginRequest.getUsername(),
                3600000 // Время жизни токена в мс (1 час)
        ));
    }

    /**
     * Простая обработка выхода пользователя из системы.
     * В JWT архитектуре, сервер не может "отозвать" токен без дополнительной инфраструктуры.
     * Основная логика - на клиенте, который должен удалить токены после успешного ответа.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String token = tokenProvider.getTokenFromRequest(request);

        if (token != null) {
            try {
                // Извлекаем имя пользователя для логирования
                String username = tokenProvider.getUsernameFromToken(token);
                log.info("Пользователь {} вышел из системы", username);
            } catch (Exception e) {
                // Если токен невалидный, просто логируем ошибку
                log.warn("Невозможно извлечь пользователя из токена при выходе: {}", e.getMessage());
            }
        } else {
            log.info("Запрос на выход без токена");
        }

        // Возвращаем успешный ответ в любом случае
        return ResponseEntity.ok(new MessageResponse("Выход из системы выполнен успешно"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequestDto registrationRequest) {
        log.info("Запрос на регистрацию пользователя: {}", registrationRequest.getUsername());

        // Проверяем, существует ли пользователь с таким именем
        if (userRepository.existsByUsername(registrationRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Ошибка: Имя пользователя уже занято"));
        }

        // Проверяем, существует ли пользователь с таким email
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Ошибка: Email уже используется"));
        }

        // Создаем нового пользователя
        User user = User.builder()
                .username(registrationRequest.getUsername())
                .email(registrationRequest.getEmail())
                .firstName(registrationRequest.getFirstName())
                .lastName(registrationRequest.getLastName())
                // Хэшируем пароль перед сохранением
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .active(true)
                .roles(Set.of("ROLE_DRIVER")) // По умолчанию назначаем роль водителя
                .build();

        // Сохраняем пользователя в БД
        userRepository.save(user);

        log.info("Пользователь {} успешно зарегистрирован", registrationRequest.getUsername());

        return ResponseEntity.ok(new MessageResponse("Пользователь успешно зарегистрирован"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponseDto> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Запрос на обновление токена");

        // Проверка валидности refresh token
        if (!tokenProvider.validateToken(request.getRefreshToken())) {
            log.warn("Попытка обновления с недействительным refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JwtResponseDto(null, null, null, 0));
        }

        // Извлечение имени пользователя из токена
        String username = tokenProvider.getUsernameFromToken(request.getRefreshToken());

        // Проверка существования пользователя
        if (!userRepository.existsByUsername(username)) {
            log.warn("Пользователь {} не найден при обновлении токена", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JwtResponseDto(null, null, null, 0));
        }

        // Генерация новых токенов
        String newAccessToken = tokenProvider.generateToken(username);
        String newRefreshToken = tokenProvider.generateRefreshToken(username);

        log.info("Токены успешно обновлены для пользователя {}", username);

        return ResponseEntity.ok(new JwtResponseDto(
                newAccessToken,
                newRefreshToken,
                username,
                3600000 // 1 час
        ));
    }
}
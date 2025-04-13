package ru.maslov.trucknavigator.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.maslov.trucknavigator.dto.auth.JwtResponseDto;
import ru.maslov.trucknavigator.dto.auth.LoginRequestDto;
import ru.maslov.trucknavigator.security.JwtTokenProvider;

import jakarta.validation.Valid;

/**
 * Контроллер для авторизации и аутентификации пользователей.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    /**
     * Авторизация пользователя и выдача JWT токена.
     *
     * @param loginRequest данные для входа (имя пользователя и пароль)
     * @return JWT токен и информация о пользователе
     */
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
                // Дополнительная информация о пользователе может быть добавлена здесь
                3600000 // Время жизни токена в мс (1 час)
        ));
    }
}
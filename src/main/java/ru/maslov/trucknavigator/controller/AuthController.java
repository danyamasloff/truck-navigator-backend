package ru.maslov.trucknavigator.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
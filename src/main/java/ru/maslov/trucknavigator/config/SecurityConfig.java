package ru.maslov.trucknavigator.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.maslov.trucknavigator.security.JwtAuthenticationFilter;
import ru.maslov.trucknavigator.security.JwtAuthenticationEntryPoint;

import java.util.Arrays;
import java.util.List;

/**
 * Конфигурация безопасности приложения.
 * Настраивает аутентификацию и авторизацию, а также правила доступа к ресурсам.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;

    /**
     * Настраивает цепочку фильтров безопасности.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()

                        // GET эндпоинты для разработки - убираем аутентификацию
                        .requestMatchers(HttpMethod.GET, "/api/routes/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/drivers/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cargos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/geocoding/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/weather/**").permitAll()

                        // POST эндпоинты для расчетов - публичные для разработки
                        .requestMatchers("/api/routes/calculate").permitAll()
                        .requestMatchers("/api/routes/plan").permitAll()
                        .requestMatchers("/api/routes/plan-by-name").permitAll()

                        // Эндпоинты для модификации данных - требуют аутентификации
                        .requestMatchers(HttpMethod.POST, "/api/vehicles/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.POST, "/api/drivers/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.POST, "/api/cargos/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.POST, "/api/routes/**").hasAnyRole("ADMIN", "DISPATCHER")

                        .requestMatchers(HttpMethod.PUT, "/api/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("ADMIN")

                        // Остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Настраивает провайдер аутентификации.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Настраивает менеджер аутентификации.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }



    /**
     * Настраивает CORS (Cross-Origin Resource Sharing).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Разрешенные источники запросов
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",      // Dev React
                "http://localhost:5173",      // Dev Vite
                "http://localhost:4173",      // Vite preview
                "https://truck-navigator.ru"  // Production
        ));

        // Разрешенные методы HTTP
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Разрешенные заголовки запросов
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Разрешить куки и заголовки авторизации
        configuration.setAllowCredentials(true);

        // Заголовки, которые клиент может использовать в ответе
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // Время кэширования предварительного запроса (preflight)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

package ru.maslov.trucknavigator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.maslov.trucknavigator.entity.User;

import java.util.Optional;

/**
 * Репозиторий для работы с пользователями
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Находит пользователя по логину
     *
     * @param username логин пользователя
     * @return опциональный объект с пользователем
     */
    Optional<User> findByUsername(String username);

    /**
     * Находит пользователя по email
     *
     * @param email email пользователя
     * @return опциональный объект с пользователем
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверяет существование пользователя с указанным логином
     *
     * @param username логин пользователя
     * @return true, если пользователь существует
     */
    boolean existsByUsername(String username);

    /**
     * Проверяет существование пользователя с указанным email
     *
     * @param email email пользователя
     * @return true, если пользователь существует
     */
    boolean existsByEmail(String email);
}

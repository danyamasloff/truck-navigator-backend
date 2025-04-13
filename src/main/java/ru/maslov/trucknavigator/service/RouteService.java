package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.repository.RouteRepository;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с маршрутами.
 */
@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;

    /**
     * Получает все маршруты.
     *
     * @return список маршрутов
     */
    public List<Route> findAll() {
        return routeRepository.findAll();
    }

    /**
     * Получает маршрут по идентификатору.
     *
     * @param id идентификатор маршрута
     * @return опциональный объект с маршрутом
     */
    public Optional<Route> findById(Long id) {
        return routeRepository.findById(id);
    }

    /**
     * Сохраняет маршрут.
     *
     * @param route маршрут для сохранения
     * @return сохраненный маршрут
     */
    public Route save(Route route) {
        return routeRepository.save(route);
    }

    /**
     * Проверяет наличие маршрута по идентификатору.
     *
     * @param id идентификатор маршрута
     * @return true, если маршрут существует
     */
    public boolean existsById(Long id) {
        return routeRepository.existsById(id);
    }

    /**
     * Удаляет маршрут по идентификатору.
     *
     * @param id идентификатор маршрута
     */
    public void deleteById(Long id) {
        routeRepository.deleteById(id);
    }
}
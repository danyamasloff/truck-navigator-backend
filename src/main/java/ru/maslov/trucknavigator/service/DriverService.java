package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.repository.DriverRepository;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с водителями.
 */
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    /**
     * Получает всех водителей.
     *
     * @return список водителей
     */
    public List<Driver> findAll() {
        return driverRepository.findAll();
    }

    /**
     * Получает водителя по идентификатору.
     *
     * @param id идентификатор водителя
     * @return опциональный объект с водителем
     */
    public Optional<Driver> findById(Long id) {
        return driverRepository.findById(id);
    }

    /**
     * Сохраняет водителя.
     *
     * @param driver водитель для сохранения
     * @return сохраненный водитель
     */
    public Driver save(Driver driver) {
        return driverRepository.save(driver);
    }

    /**
     * Проверяет наличие водителя по идентификатору.
     *
     * @param id идентификатор водителя
     * @return true, если водитель существует
     */
    public boolean existsById(Long id) {
        return driverRepository.existsById(id);
    }

    /**
     * Удаляет водителя по идентификатору.
     *
     * @param id идентификатор водителя
     */
    public void deleteById(Long id) {
        driverRepository.deleteById(id);
    }
}
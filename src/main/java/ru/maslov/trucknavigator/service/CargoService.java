package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.repository.CargoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с грузами.
 */
@Service
@RequiredArgsConstructor
public class CargoService {

    private final CargoRepository cargoRepository;

    /**
     * Получает все грузы.
     *
     * @return список грузов
     */
    public List<Cargo> findAll() {
        return cargoRepository.findAll();
    }

    /**
     * Получает груз по идентификатору.
     *
     * @param id идентификатор груза
     * @return опциональный объект с грузом
     */
    public Optional<Cargo> findById(Long id) {
        return cargoRepository.findById(id);
    }

    /**
     * Сохраняет груз.
     *
     * @param cargo груз для сохранения
     * @return сохраненный груз
     */
    public Cargo save(Cargo cargo) {
        return cargoRepository.save(cargo);
    }

    /**
     * Проверяет наличие груза по идентификатору.
     *
     * @param id идентификатор груза
     * @return true, если груз существует
     */
    public boolean existsById(Long id) {
        return cargoRepository.existsById(id);
    }

    /**
     * Удаляет груз по идентификатору.
     *
     * @param id идентификатор груза
     */
    public void deleteById(Long id) {
        cargoRepository.deleteById(id);
    }
}
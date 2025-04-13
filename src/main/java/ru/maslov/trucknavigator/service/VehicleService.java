package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.repository.VehicleRepository;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с транспортными средствами.
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    /**
     * Получает все транспортные средства.
     *
     * @return список транспортных средств
     */
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    /**
     * Получает транспортное средство по идентификатору.
     *
     * @param id идентификатор ТС
     * @return опциональный объект с ТС
     */
    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id);
    }

    /**
     * Сохраняет транспортное средство.
     *
     * @param vehicle ТС для сохранения
     * @return сохраненное ТС
     */
    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    /**
     * Проверяет наличие ТС по идентификатору.
     *
     * @param id идентификатор ТС
     * @return true, если ТС существует
     */
    public boolean existsById(Long id) {
        return vehicleRepository.existsById(id);
    }

    /**
     * Удаляет ТС по идентификатору.
     *
     * @param id идентификатор ТС
     */
    public void deleteById(Long id) {
        vehicleRepository.deleteById(id);
    }
}
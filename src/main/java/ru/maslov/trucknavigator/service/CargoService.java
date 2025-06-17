package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.cargo.CargoDetailDto;
import ru.maslov.trucknavigator.dto.cargo.CargoSummaryDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.repository.CargoRepository;
import ru.maslov.trucknavigator.mapper.CargoMapper;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с грузами.
 */
@Service
@RequiredArgsConstructor
public class CargoService {

    private final CargoRepository cargoRepository;
    private final CargoMapper cargoMapper;
    private final NotificationService notificationService;

    /**
     * Получает все грузы в виде сокращенных DTO.
     *
     * @return список DTO грузов
     */
    public List<CargoSummaryDto> findAllSummaries() {
        List<Cargo> cargos = cargoRepository.findAll();
        return cargoMapper.toSummaryDtoList(cargos);
    }

    /**
     * Получает груз по идентификатору в виде полного DTO.
     *
     * @param id идентификатор груза
     * @return детальное DTO с информацией о грузе
     * @throws EntityNotFoundException если груз не найден
     */
    public CargoDetailDto findDetailById(Long id) {
        Cargo cargo = cargoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cargo", id));
        return cargoMapper.toDetailDto(cargo);
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
     * Сохраняет груз и возвращает его детальное DTO.
     *
     * @param cargo груз для сохранения
     * @return детальное DTO сохраненного груза
     */
    @Transactional
    public CargoDetailDto saveAndGetDto(Cargo cargo) {
        boolean isNewCargo = cargo.getId() == null;
        Cargo savedCargo = cargoRepository.save(cargo);
        
        if (isNewCargo) {
            // Создаем уведомление о создании нового груза
            notificationService.notifyCargoCreated(savedCargo.getId(), savedCargo.getName());
        } else {
            // Создаем уведомление об обновлении груза
            notificationService.notifyCargoUpdated(savedCargo.getId(), savedCargo.getName());
        }
        
        return cargoMapper.toDetailDto(savedCargo);
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
     * @throws EntityNotFoundException если груз не найден
     */
    @Transactional
    public void deleteById(Long id) {
        if (!cargoRepository.existsById(id)) {
            throw new EntityNotFoundException("Cargo", id);
        }
        cargoRepository.deleteById(id);
    }
}

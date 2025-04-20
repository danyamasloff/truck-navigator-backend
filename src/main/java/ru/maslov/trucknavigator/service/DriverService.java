package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.driver.DriverDetailDto;
import ru.maslov.trucknavigator.dto.driver.DriverSummaryDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.repository.DriverRepository;
import ru.maslov.trucknavigator.service.mapper.DriverMapper;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с водителями.
 */
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    /**
     * Получает всех водителей в виде сокращенных DTO.
     *
     * @return список DTO водителей
     */
    public List<DriverSummaryDto> findAllSummaries() {
        List<Driver> drivers = driverRepository.findAll();
        return driverMapper.toSummaryDtoList(drivers);
    }

    /**
     * Получает водителя по идентификатору в виде полного DTO.
     *
     * @param id идентификатор водителя
     * @return детальное DTO с информацией о водителе
     * @throws EntityNotFoundException если водитель не найден
     */
    public DriverDetailDto findDetailById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Driver", id));
        return driverMapper.toDetailDto(driver);
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
     * Сохраняет водителя и возвращает его детальное DTO.
     *
     * @param driver водитель для сохранения
     * @return детальное DTO сохраненного водителя
     */
    @Transactional
    public DriverDetailDto saveAndGetDto(Driver driver) {
        Driver savedDriver = driverRepository.save(driver);
        return driverMapper.toDetailDto(savedDriver);
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
     * @throws EntityNotFoundException если водитель не найден
     */
    @Transactional
    public void deleteById(Long id) {
        if (!driverRepository.existsById(id)) {
            throw new EntityNotFoundException("Driver", id);
        }
        driverRepository.deleteById(id);
    }
}
package ru.maslov.trucknavigator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.vehicle.VehicleDetailDto;
import ru.maslov.trucknavigator.dto.vehicle.VehicleSummaryDto;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.repository.VehicleRepository;
import ru.maslov.trucknavigator.mapper.VehicleMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с транспортными средствами.
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final NotificationService notificationService;

    /**
     * Получает все транспортные средства в виде сокращенных DTO.
     *
     * @return список DTO транспортных средств
     */
    public List<VehicleSummaryDto> findAllSummaries() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        return vehicleMapper.toSummaryDtoList(vehicles);
    }

    /**
     * Получает транспортное средство по идентификатору в виде полного DTO.
     *
     * @param id идентификатор ТС
     * @return детальное DTO с информацией о ТС
     * @throws EntityNotFoundException если ТС не найдено
     */
    public VehicleDetailDto findDetailById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", id));
        return vehicleMapper.toDetailDto(vehicle);
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
     * Сохраняет ТС и возвращает его детальное DTO.
     *
     * @param vehicle ТС для сохранения
     * @return детальное DTO сохраненного ТС
     */
    @Transactional
    public VehicleDetailDto saveAndGetDto(Vehicle vehicle) {
        boolean isNewVehicle = vehicle.getId() == null;
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        
        if (isNewVehicle) {
            // Создаем уведомление о создании нового ТС
            notificationService.notifyVehicleCreated(savedVehicle.getId(), savedVehicle.getRegistrationNumber());
        } else {
            // Создаем уведомление об обновлении ТС
            notificationService.notifyVehicleUpdated(savedVehicle.getId(), savedVehicle.getRegistrationNumber());
        }
        
        return vehicleMapper.toDetailDto(savedVehicle);
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
     * @throws EntityNotFoundException если ТС не найдено
     */
    @Transactional
    public void deleteById(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new EntityNotFoundException("Vehicle", id);
        }
        vehicleRepository.deleteById(id);
    }

    /**
     * Обновляет уровень топлива транспортного средства и возвращает детальное DTO.
     *
     * @param id идентификатор ТС
     * @param fuelLevel новый уровень топлива
     * @return детальное DTO обновленного ТС
     * @throws EntityNotFoundException если ТС не найдено
     */
    @Transactional
    public VehicleDetailDto updateFuelLevel(Long id, BigDecimal fuelLevel) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", id));

        vehicle.setCurrentFuelLevelLitres(fuelLevel);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return vehicleMapper.toDetailDto(savedVehicle);
    }

    /**
     * Обновляет показания одометра ТС и возвращает детальное DTO.
     *
     * @param id идентификатор ТС
     * @param odometerValue новое значение одометра
     * @return детальное DTO обновленного ТС
     * @throws EntityNotFoundException если ТС не найдено
     */
    @Transactional
    public VehicleDetailDto updateOdometer(Long id, BigDecimal odometerValue) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", id));

        vehicle.setCurrentOdometerKm(odometerValue);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return vehicleMapper.toDetailDto(savedVehicle);
    }
}

package ru.maslov.trucknavigator.service;
import ru.maslov.trucknavigator.entity.DrivingStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maslov.trucknavigator.dto.driver.DriverDetailDto;
import ru.maslov.trucknavigator.dto.driver.DriverMedicalDto;
import ru.maslov.trucknavigator.dto.driver.DriverQualificationDto;
import ru.maslov.trucknavigator.dto.driver.DriverSummaryDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.DrivingStatus;
import ru.maslov.trucknavigator.exception.EntityNotFoundException;
import ru.maslov.trucknavigator.mapper.DriverMapper;
import ru.maslov.trucknavigator.repository.DriverRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для работы с водителями.
 * Содержит бизнес-логику для управления данными водителей,
 * включая режим труда и отдыха (РТО), медицинские данные и квалификации.
 */
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    /**
     * Получает список всех водителей в виде сокращенных DTO.
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

    /**
     * Находит водителя по идентификатору.
     *
     * @param id идентификатор водителя
     * @return Optional с водителем или пустой Optional, если водитель не найден
     */
    public Optional<Driver> findById(Long id) {
        return driverRepository.findById(id);
    }

    /**
     * Обновляет квалификационные документы водителя.
     *
     * @param driverId идентификатор водителя
     * @param qualifications данные о квалификационных документах
     * @return обновленные данные о квалификационных документах
     */
    @Transactional
    public DriverQualificationDto updateDriverQualifications(Long driverId, DriverQualificationDto qualifications) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        // Обновление данных о квалификации водителя
        driver.setLicenseNumber(qualifications.getLicenseNumber());
        driver.setLicenseIssueDate(qualifications.getIssueDate());
        driver.setLicenseExpiryDate(qualifications.getExpiryDate());
        driver.setLicenseCategories(qualifications.getCategories());
        driver.setHasDangerousGoodsCertificate(qualifications.isHasDangerousGoodsCertificate());
        driver.setDangerousGoodsCertificateExpiry(qualifications.getDangerousGoodsExpiryDate());
        driver.setHasInternationalTransportationPermit(qualifications.isHasInternationalPermit());

        Driver savedDriver = driverRepository.save(driver);

        // Преобразуем сущность обратно в DTO и возвращаем результат
        return mapToQualificationDto(savedDriver);
    }

    /**
     * Получает данные о квалификационных документах водителя.
     *
     * @param driverId идентификатор водителя
     * @return данные о квалификационных документах
     */
    public DriverQualificationDto getDriverQualifications2(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        return mapToQualificationDto(driver);
    }

    /**
     * Получает квалификации водителя в виде набора строк.
     *
     * @param driverId идентификатор водителя
     * @return набор квалификаций
     */
    public Set<String> getDriverQualifications(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        Set<String> qualifications = new HashSet<>();

        // Добавление категорий водительского удостоверения
        if (driver.getLicenseCategories() != null && !driver.getLicenseCategories().isEmpty()) {
            qualifications.addAll(Arrays.asList(driver.getLicenseCategories().split(",")));
        }

        // Добавление дополнительных квалификаций
        if (driver.isHasDangerousGoodsCertificate()) {
            qualifications.add("DANGEROUS_GOODS");
        }

        if (driver.isHasInternationalTransportationPermit()) {
            qualifications.add("INTERNATIONAL");
        }

        return qualifications;
    }

    /**
     * Обновляет квалификации водителя.
     *
     * @param driverId идентификатор водителя
     * @param qualifications набор квалификаций
     * @return обновленный набор квалификаций
     */
    @Transactional
    public Set<String> updateDriverQualifications(Long driverId, Set<String> qualifications) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        // Разделяем на стандартные категории и особые разрешения
        Set<String> standardCategories = new HashSet<>();
        boolean hasDangerousGoods = false;
        boolean hasInternational = false;

        for (String qualification : qualifications) {
            if ("DANGEROUS_GOODS".equals(qualification)) {
                hasDangerousGoods = true;
            } else if ("INTERNATIONAL".equals(qualification)) {
                hasInternational = true;
            } else {
                standardCategories.add(qualification);
            }
        }

        // Обновляем водителя
        driver.setLicenseCategories(String.join(",", standardCategories));
        driver.setHasDangerousGoodsCertificate(hasDangerousGoods);
        driver.setHasInternationalTransportationPermit(hasInternational);

        driverRepository.save(driver);

        return getDriverQualifications(driverId);
    }

    /**
     * Обновляет медицинские данные водителя.
     *
     * @param driverId идентификатор водителя
     * @param medical данные о медицинском допуске
     * @return обновленные медицинские данные
     */
    @Transactional
    public DriverMedicalDto updateDriverMedical(Long driverId, DriverMedicalDto medical) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        // Обновление медицинских данных водителя
        driver.setMedicalCertificateNumber(medical.getCertificateNumber());
        driver.setMedicalCertificateIssueDate(medical.getIssueDate());
        driver.setMedicalCertificateExpiryDate(medical.getExpiryDate());
        driver.setMedicalRestrictions(medical.getRestrictions());

        Driver savedDriver = driverRepository.save(driver);

        // Преобразуем сущность обратно в DTO и возвращаем результат
        return mapToMedicalDto(savedDriver);
    }

    /**
     * Получает медицинские данные водителя.
     *
     * @param driverId идентификатор водителя
     * @return данные о медицинском допуске
     */
    public DriverMedicalDto getDriverMedical(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        return mapToMedicalDto(driver);
    }

    /**
     * Обновляет статус водителя (режим труда и отдыха).
     *
     * @param driverId идентификатор водителя
     * @param status новый статус водителя
     * @param timestamp время изменения статуса
     * @return обновленный водитель в виде детального DTO
     */
    @Transactional
    public DriverDetailDto updateDriverStatus(Long driverId, DrivingStatus status, LocalDateTime timestamp) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("Driver", driverId));

        // Обновление статуса вождения
        driver.setCurrentDrivingStatus(status);
        driver.setCurrentStatusStartTime(timestamp);

        // Дополнительная логика обновления счетчиков вождения в зависимости от статуса
        updateDrivingCounters(driver, status);

        Driver savedDriver = driverRepository.save(driver);

        return driverMapper.toDetailDto(savedDriver);
    }

    // Вспомогательные методы

    private DriverQualificationDto mapToQualificationDto(Driver driver) {
        return new DriverQualificationDto(
                driver.getLicenseNumber(),
                driver.getLicenseIssueDate(),
                driver.getLicenseExpiryDate(),
                driver.getLicenseCategories(),
                driver.isHasDangerousGoodsCertificate(),
                driver.getDangerousGoodsCertificateExpiry(),
                driver.isHasInternationalTransportationPermit()
        );
    }

    private DriverMedicalDto mapToMedicalDto(Driver driver) {
        return new DriverMedicalDto(
                driver.getMedicalCertificateNumber(),
                driver.getMedicalCertificateIssueDate(),
                driver.getMedicalCertificateExpiryDate(),
                driver.getMedicalRestrictions()
        );
    }

    private void updateDrivingCounters(Driver driver, DrivingStatus newStatus) {
        DrivingStatus oldStatus = driver.getCurrentDrivingStatus();

        // Рассчитать время в предыдущем статусе
        if (oldStatus != null && driver.getCurrentStatusStartTime() != null) {
            long minutesInPreviousStatus = java.time.Duration.between(
                    driver.getCurrentStatusStartTime(),
                    LocalDateTime.now()
            ).toMinutes();

            // Обновляем счетчики в зависимости от предыдущего статуса
            if (DrivingStatus.DRIVING.equals(oldStatus)) {
                // Увеличиваем счетчики времени вождения
                updateDrivingTimeCounters(driver, minutesInPreviousStatus);
            }
        }

        // Сбрасываем счетчики при переходе в режим отдыха
        if (isRestStatus(newStatus)) {
            resetCountersBasedOnRestType(driver, newStatus);
        }
    }

    private void updateDrivingTimeCounters(Driver driver, long minutesInPreviousStatus) {
        // Обновляем счетчик непрерывного вождения
        driver.setContinuousDrivingMinutes(
                (driver.getContinuousDrivingMinutes() != null ?
                        driver.getContinuousDrivingMinutes() : 0) + (int)minutesInPreviousStatus
        );

        // Обновляем счетчик суточного вождения
        driver.setDailyDrivingMinutesToday(
                (driver.getDailyDrivingMinutesToday() != null ?
                        driver.getDailyDrivingMinutesToday() : 0) + (int)minutesInPreviousStatus
        );

        // Обновляем счетчик недельного вождения
        driver.setWeeklyDrivingMinutes(
                (driver.getWeeklyDrivingMinutes() != null ?
                        driver.getWeeklyDrivingMinutes() : 0) + (int)minutesInPreviousStatus
        );
    }

    private boolean isRestStatus(DrivingStatus status) {
        return DrivingStatus.REST_BREAK.equals(status) ||
                DrivingStatus.DAILY_REST.equals(status) ||
                DrivingStatus.WEEKLY_REST.equals(status);
    }

    private void resetCountersBasedOnRestType(Driver driver, DrivingStatus restStatus) {
        // Сброс счетчика непрерывного вождения при любом отдыхе
        driver.setContinuousDrivingMinutes(0);

        // Сброс суточного счетчика при дневном или недельном отдыхе
        if (DrivingStatus.DAILY_REST.equals(restStatus)) {
            driver.setDailyDrivingMinutesToday(0);
        }

        // Сброс недельного счетчика при недельном отдыхе
        if (DrivingStatus.WEEKLY_REST.equals(restStatus)) {
            driver.setWeeklyDrivingMinutes(0);
        }
    }
}

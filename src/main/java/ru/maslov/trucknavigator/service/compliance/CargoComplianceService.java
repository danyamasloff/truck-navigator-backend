package ru.maslov.trucknavigator.service.compliance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.compliance.ComplianceResultDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Vehicle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CargoComplianceService {

    /**
     * Проверяет возможность перевозки груза указанным ТС.
     *
     * @param cargo груз
     * @param vehicle транспортное средство
     * @return результат проверки соответствия
     */
    public ComplianceResultDto checkCargoVehicleCompliance(Cargo cargo, Vehicle vehicle) {
        List<String> warnings = new ArrayList<>();
        boolean isCompliant = true;

        // Проверка для опасных грузов
        if (cargo.isDangerous() && !vehicle.isHasDangerousGoodsPermission()) {
            warnings.add("Транспортное средство не имеет разрешения на перевозку опасных грузов");
            isCompliant = false;
        }

        // Проверка для скоропортящихся грузов, требующих температурного контроля
        if (cargo.isRequiresTemperatureControl() && !vehicle.isHasRefrigerator()) {
            warnings.add("Требуется транспортное средство с рефрижератором");
            isCompliant = false;
        }

        // Проверка для негабаритных грузов
        if (cargo.isOversized() && !vehicle.isHasOversizedCargoPermission()) {
            warnings.add("Транспортное средство не имеет разрешения на перевозку негабаритных грузов");
            isCompliant = false;
        }

        // Проверка габаритов груза
        if (cargo.getLengthCm() != null && cargo.getWidthCm() != null &&
                cargo.getHeightCm() != null &&
                vehicle.getLengthCm() != null && vehicle.getWidthCm() != null &&
                vehicle.getHeightCm() != null) {

            if (cargo.getLengthCm() > vehicle.getLengthCm()) {
                warnings.add("Длина груза превышает длину грузового отсека ТС");
                isCompliant = false;
            }

            if (cargo.getWidthCm() > vehicle.getWidthCm()) {
                warnings.add("Ширина груза превышает ширину грузового отсека ТС");
                isCompliant = false;
            }

            if (cargo.getHeightCm() > vehicle.getHeightCm()) {
                warnings.add("Высота груза превышает высоту грузового отсека ТС");
                isCompliant = false;
            }
        }

        // Проверка грузоподъемности
        if (cargo.getWeightKg() != null && vehicle.getMaxLoadCapacityKg() != null) {
            if (cargo.getWeightKg() > vehicle.getMaxLoadCapacityKg()) {
                warnings.add("Вес груза превышает грузоподъемность ТС");
                isCompliant = false;
            }
        }

        return ComplianceResultDto.builder()
                .compliant(isCompliant)
                .warnings(warnings)
                .build();
    }

    /**
     * Проверяет наличие у водителя необходимых допусков для перевозки груза.
     *
     * @param cargo груз
     * @param driver водитель
     * @return результат проверки соответствия
     */
    public ComplianceResultDto checkCargoDriverCompliance(Cargo cargo, Driver driver) {
        List<String> warnings = new ArrayList<>();
        boolean isCompliant = true;

        // Проверка для опасных грузов
        if (cargo.isDangerous() && !driver.isHasDangerousGoodsCertificate()) {
            warnings.add("Водитель не имеет допуска на перевозку опасных грузов");
            isCompliant = false;
        }

        // Проверка срока действия сертификата ADR
        if (cargo.isDangerous() && driver.isHasDangerousGoodsCertificate() &&
                driver.getDangerousGoodsCertificateExpiry() != null) {

            if (driver.getDangerousGoodsCertificateExpiry().isBefore(LocalDate.now())) {
                warnings.add("Истек срок действия сертификата ADR водителя");
                isCompliant = false;
            }
        }

        // Проверка соответствия классов ADR
        if (cargo.isDangerous() && cargo.getDangerousGoodsClass() != null &&
                driver.getAdrClasses() != null) {

            if (!driver.getAdrClasses().contains(cargo.getDangerousGoodsClass())) {
                warnings.add("Водитель не имеет допуска для перевозки опасных грузов класса " +
                        cargo.getDangerousGoodsClass());
                isCompliant = false;
            }
        }

        // Проверка для негабаритных грузов
        if (cargo.isOversized() && !driver.isHasOversizedCargoPermit()) {
            warnings.add("Водитель не имеет допуска на перевозку негабаритных грузов");
            isCompliant = false;
        }

        // Проверка для скоропортящихся грузов
        if (cargo.isRequiresTemperatureControl() && !driver.isHasRefrigeratedCargoPermit()) {
            warnings.add("Водитель не имеет опыта перевозки грузов с температурным режимом");
            isCompliant = false;
        }

        // Проверка срока действия медицинской справки
        if (driver.getMedicalCertificateExpiryDate() != null &&
                driver.getMedicalCertificateExpiryDate().isBefore(LocalDate.now())) {
            warnings.add("Истек срок действия медицинской справки водителя");
            isCompliant = false;
        }

        return ComplianceResultDto.builder()
                .compliant(isCompliant)
                .warnings(warnings)
                .build();
    }
}

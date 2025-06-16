package ru.maslov.trucknavigator.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.analytics.RiskFactorDto;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.entity.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для анализа рисков маршрута.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceRiskAnalysisService {

    /**
     * Анализирует риски маршрута.
     *
     * @param route маршрут
     * @param vehicle транспортное средство
     * @param driver водитель
     * @param cargo груз
     * @return список факторов риска
     */
    public List<RiskFactorDto> analyzeRouteRisks(Route route, Vehicle vehicle, Driver driver, Cargo cargo) {
        List<RiskFactorDto> riskFactors = new ArrayList<>();

        log.debug("Анализ рисков для маршрута: {}", route.getName());

        // Проверка рисков, связанных с маршрутом
        analyzeRouteSpecificRisks(route, riskFactors);

        // Проверка рисков, связанных с транспортным средством
        if (vehicle != null) {
            analyzeVehicleRisks(vehicle, route, riskFactors);
        }

        // Проверка рисков, связанных с водителем
        if (driver != null) {
            analyzeDriverRisks(driver, route, riskFactors);
        }

        // Проверка рисков, связанных с грузом
        if (cargo != null) {
            analyzeCargoRisks(cargo, vehicle, route, riskFactors);
        }

        return riskFactors;
    }

    /**
     * Анализирует риски, связанные с маршрутом.
     *
     * @param route маршрут
     * @param riskFactors список факторов риска
     */
    private void analyzeRouteSpecificRisks(Route route, List<RiskFactorDto> riskFactors) {
        // Проверка длины маршрута
        if (route.getDistanceKm() != null && route.getDistanceKm().compareTo(BigDecimal.valueOf(500)) > 0) {
            riskFactors.add(RiskFactorDto.builder()
                    .category("ROUTE")
                    .type("LONG_DISTANCE")
                    .description("Большая протяжённость маршрута (> 500 км)")
                    .severity("MEDIUM")
                    .riskScore(40)
                    .recommendation("Рассмотреть возможность разделения маршрута на несколько дней")
                    .build());
        }

        // Проверка времени в пути
        if (route.getEstimatedDurationMinutes() != null && route.getEstimatedDurationMinutes() > 480) {
            riskFactors.add(RiskFactorDto.builder()
                    .category("ROUTE")
                    .type("LONG_DURATION")
                    .description("Большая продолжительность поездки (> 8 часов)")
                    .severity("HIGH")
                    .riskScore(60)
                    .recommendation("Разделить маршрут на несколько дней или назначить дополнительного водителя")
                    .build());
        }

        // Проверка ночного вождения
        if (route.getDepartureTime() != null) {
            int departureHour = route.getDepartureTime().getHour();
            int estimatedDurationHours = 0;

            if (route.getEstimatedDurationMinutes() != null) {
                estimatedDurationHours = route.getEstimatedDurationMinutes() / 60;
            }

            if (departureHour >= 20 || departureHour <= 5 ||
                    (departureHour + estimatedDurationHours) % 24 <= 5) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("ROUTE")
                        .type("NIGHT_DRIVING")
                        .description("Маршрут предполагает вождение в ночное время")
                        .severity("MEDIUM")
                        .riskScore(45)
                        .recommendation("По возможности избегать вождения между 00:00 и 06:00")
                        .build());
            }
        }
    }

    /**
     * Анализирует риски, связанные с транспортным средством.
     *
     * @param vehicle транспортное средство
     * @param route маршрут
     * @param riskFactors список факторов риска
     */
    private void analyzeVehicleRisks(Vehicle vehicle, Route route, List<RiskFactorDto> riskFactors) {
        // Проверка возраста ТС
        if (vehicle.getProductionYear() != null) {
            int vehicleAge = LocalDate.now().getYear() - vehicle.getProductionYear();

            if (vehicleAge > 10) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("VEHICLE")
                        .type("OLD_VEHICLE")
                        .description("Возраст ТС более 10 лет")
                        .severity("HIGH")
                        .riskScore(65)
                        .recommendation("Рекомендуется тщательная проверка ТС перед поездкой")
                        .build());
            } else if (vehicleAge > 7) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("VEHICLE")
                        .type("AGING_VEHICLE")
                        .description("Возраст ТС более 7 лет")
                        .severity("MEDIUM")
                        .riskScore(40)
                        .recommendation("Рекомендуется дополнительная проверка технического состояния")
                        .build());
            }
        }

        // Проверка уровня топлива
        if (vehicle.getCurrentFuelLevelLitres() != null && vehicle.getFuelCapacityLitres() != null
                && vehicle.getFuelCapacityLitres() > 0 && route.getDistanceKm() != null
                && vehicle.getFuelConsumptionPer100km() != null) {

            // Расчет требуемого топлива для маршрута
            BigDecimal requiredFuel = route.getDistanceKm()
                    .multiply(vehicle.getFuelConsumptionPer100km())
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

            // Если текущего топлива недостаточно для маршрута
            if (vehicle.getCurrentFuelLevelLitres().compareTo(requiredFuel) < 0) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("VEHICLE")
                        .type("INSUFFICIENT_FUEL")
                        .description("Недостаточный уровень топлива для завершения маршрута")
                        .severity("HIGH")
                        .riskScore(70)
                        .recommendation("Заправьте автомобиль перед началом поездки")
                        .build());
            }

            // Если текущего топлива хватает, но запас мал (менее 20%)
            BigDecimal fuelReserve = vehicle.getCurrentFuelLevelLitres().subtract(requiredFuel);
            BigDecimal reservePercentage = fuelReserve
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(vehicle.getFuelCapacityLitres()), 2, BigDecimal.ROUND_HALF_UP);

            if (reservePercentage.compareTo(BigDecimal.valueOf(20)) < 0 &&
                    fuelReserve.compareTo(BigDecimal.ZERO) > 0) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("VEHICLE")
                        .type("LOW_FUEL_RESERVE")
                        .description("Малый запас топлива после завершения маршрута (менее 20%)")
                        .severity("MEDIUM")
                        .riskScore(40)
                        .recommendation("Рекомендуется заправиться до начала поездки")
                        .build());
            }
        }
    }

    /**
     * Анализирует риски, связанные с водителем.
     *
     * @param driver водитель
     * @param route маршрут
     * @param riskFactors список факторов риска
     */
    private void analyzeDriverRisks(Driver driver, Route route, List<RiskFactorDto> riskFactors) {
        // Проверка опыта вождения
        if (driver.getDrivingExperienceYears() != null && driver.getDrivingExperienceYears() < 3) {
            riskFactors.add(RiskFactorDto.builder()
                    .category("DRIVER")
                    .type("INEXPERIENCED_DRIVER")
                    .description("Малый опыт вождения (менее 3 лет)")
                    .severity("HIGH")
                    .riskScore(60)
                    .recommendation("Рекомендуется более осторожное вождение и дополнительный контроль")
                    .build());
        }

        // Проверка срока действия прав
        if (driver.getLicenseExpiryDate() != null) {
            long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), driver.getLicenseExpiryDate());

            if (daysUntilExpiry < 0) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("DRIVER")
                        .type("EXPIRED_LICENSE")
                        .description("Истек срок действия водительского удостоверения")
                        .severity("CRITICAL")
                        .riskScore(100)
                        .recommendation("Необходимо срочно обновить водительское удостоверение")
                        .build());
            } else if (daysUntilExpiry < 30) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("DRIVER")
                        .type("LICENSE_EXPIRING_SOON")
                        .description("Срок действия водительского удостоверения истекает в течение 30 дней")
                        .severity("HIGH")
                        .riskScore(70)
                        .recommendation("Рекомендуется своевременно обновить водительское удостоверение")
                        .build());
            }
        }

        // Проверка накопленного времени вождения
        if (driver.getDailyDrivingMinutesToday() != null && driver.getDailyDrivingMinutesToday() > 480
                && route.getEstimatedDurationMinutes() != null) {

            riskFactors.add(RiskFactorDto.builder()
                    .category("DRIVER")
                    .type("EXCESSIVE_DAILY_DRIVING")
                    .description("Превышено рекомендуемое время вождения в течение дня")
                    .severity("HIGH")
                    .riskScore(75)
                    .recommendation("Рекомендуется отдых перед началом поездки")
                    .build());
        }
    }

    /**
     * Анализирует риски, связанные с грузом.
     *
     * @param cargo груз
     * @param vehicle транспортное средство
     * @param route маршрут
     * @param riskFactors список факторов риска
     */
    private void analyzeCargoRisks(Cargo cargo, Vehicle vehicle, Route route, List<RiskFactorDto> riskFactors) {
        // Проверка опасных грузов
        if (cargo.isDangerous()) {
            riskFactors.add(RiskFactorDto.builder()
                    .category("CARGO")
                    .type("DANGEROUS_GOODS")
                    .description("Перевозка опасных грузов")
                    .severity("HIGH")
                    .riskScore(80)
                    .recommendation("Соблюдайте специальные требования к перевозке опасных грузов")
                    .build());

            // Проверка специального разрешения
            if (vehicle != null && !vehicle.isHasDangerousGoodsPermission()) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("CARGO")
                        .type("NO_DANGEROUS_GOODS_PERMISSION")
                        .description("ТС не имеет разрешения на перевозку опасных грузов")
                        .severity("CRITICAL")
                        .riskScore(100)
                        .recommendation("Необходимо использовать ТС с соответствующим разрешением")
                        .build());
            }
        }

        // Проверка скоропортящихся грузов
        if (cargo.isPerishable()) {
            riskFactors.add(RiskFactorDto.builder()
                    .category("CARGO")
                    .type("PERISHABLE_GOODS")
                    .description("Перевозка скоропортящихся грузов")
                    .severity("MEDIUM")
                    .riskScore(50)
                    .recommendation("Соблюдайте требования к перевозке скоропортящихся грузов")
                    .build());

            // Проверка наличия рефрижератора
            if (vehicle != null && cargo.isRequiresTemperatureControl() && !vehicle.isHasRefrigerator()) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("CARGO")
                        .type("NO_REFRIGERATION")
                        .description("ТС не оборудовано рефрижератором для груза, требующего температурного контроля")
                        .severity("CRITICAL")
                        .riskScore(90)
                        .recommendation("Необходимо использовать ТС с рефрижератором")
                        .build());
            }
        }

        // Проверка негабаритных грузов
        if (cargo.isOversized()) {
            riskFactors.add(RiskFactorDto.builder()
                    .category("CARGO")
                    .type("OVERSIZED_CARGO")
                    .description("Перевозка негабаритного груза")
                    .severity("HIGH")
                    .riskScore(70)
                    .recommendation("Соблюдайте специальные требования к перевозке негабаритных грузов")
                    .build());

            // Проверка специального разрешения
            if (vehicle != null && !vehicle.isHasOversizedCargoPermission()) {
                riskFactors.add(RiskFactorDto.builder()
                        .category("CARGO")
                        .type("NO_OVERSIZED_PERMISSION")
                        .description("ТС не имеет разрешения на перевозку негабаритных грузов")
                        .severity("CRITICAL")
                        .riskScore(95)
                        .recommendation("Необходимо использовать ТС с соответствующим разрешением")
                        .build());
            }
        }
    }
}

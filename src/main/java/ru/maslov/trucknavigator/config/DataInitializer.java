package ru.maslov.trucknavigator.config;
import ru.maslov.trucknavigator.entity.DrivingStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.maslov.trucknavigator.entity.*;
import ru.maslov.trucknavigator.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Компонент для инициализации тестовых данных при запуске приложения в dev-профиле.
 * Создает пользователей, водителей, транспортные средства, грузы и маршруты для тестирования.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CargoRepository cargoRepository;
    private final RouteRepository routeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("🚀 Начало инициализации тестовых данных для TruckNavigator...");

            // Инициализируем данные поэтапно с проверками
            if (userRepository.count() == 0) {
                initUsers();
                log.info("✅ Пользователи созданы: {}", userRepository.count());
            }

            if (driverRepository.count() == 0) {
                initDrivers();
                log.info("✅ Водители созданы: {}", driverRepository.count());
            }

            if (vehicleRepository.count() == 0) {
                initVehicles();
                log.info("✅ ТС созданы: {}", vehicleRepository.count());
            }

            if (cargoRepository.count() == 0) {
                initCargos();
                log.info("✅ Грузы созданы: {}", cargoRepository.count());
            }

            if (routeRepository.count() == 0) {
                initRoutes();
                log.info("✅ Маршруты созданы: {}", routeRepository.count());
            }

            log.info("🎉 Инициализация тестовых данных завершена!");
            logTestingInstructions();

        } catch (Exception e) {
            log.error("❌ Ошибка при инициализации тестовых данных: {}", e.getMessage(), e);
            // Не прерываем запуск приложения, только логируем ошибку
        }
    }

    /**
     * Инициализирует тестовых пользователей с различными ролями
     */
    private void initUsers() {
        log.info("👥 Создание тестовых пользователей...");

        List<User> users = List.of(
            User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@truck-navigator.ru")
                .firstName("Администратор")
                .lastName("Системы")
                .active(true)
                .roles(Set.of("ROLE_ADMIN"))
                .createdAt(LocalDateTime.now())
                .build(),

            User.builder()
                .username("dispatcher")
                .password(passwordEncoder.encode("disp123"))
                .email("dispatcher@truck-navigator.ru")
                .firstName("Александр")
                .lastName("Диспетчеров")
                .active(true)
                .roles(Set.of("ROLE_DISPATCHER"))
                .createdAt(LocalDateTime.now())
                .build(),

            User.builder()
                .username("manager")
                .password(passwordEncoder.encode("manager123"))
                .email("manager@truck-navigator.ru")
                .firstName("Елена")
                .lastName("Менеджерова")
                .active(true)
                .roles(Set.of("ROLE_MANAGER"))
                .createdAt(LocalDateTime.now())
                .build(),

            User.builder()
                .username("driver")
                .password(passwordEncoder.encode("driver123"))
                .email("driver@truck-navigator.ru")
                .firstName("Иван")
                .lastName("Водителев")
                .active(true)
                .roles(Set.of("ROLE_DRIVER"))
                .createdAt(LocalDateTime.now())
                .build(),

            User.builder()
                .username("supervisor")
                .password(passwordEncoder.encode("super123"))
                .email("supervisor@truck-navigator.ru")
                .firstName("Михаил")
                .lastName("Супервайзеров")
                .active(true)
                .roles(Set.of("ROLE_DISPATCHER", "ROLE_MANAGER"))
                .createdAt(LocalDateTime.now())
                .build()
        );

        userRepository.saveAll(users);
    }

    /**
     * Инициализирует тестовых водителей
     */
    private void initDrivers() {
        log.info("🚛 Создание тестовых водителей...");

        List<Driver> drivers = List.of(
            Driver.builder()
                .firstName("Иван")
                .lastName("Петров")
                .middleName("Сергеевич")
                .birthDate(LocalDate.of(1985, 5, 15))
                .licenseNumber("7722334455")
                .licenseIssueDate(LocalDate.of(2010, 3, 20))
                .licenseExpiryDate(LocalDate.of(2030, 3, 20))
                .licenseCategories("B,C,CE")
                .phoneNumber("+7-900-123-45-67")
                .email("ivan.petrov@example.com")
                .drivingExperienceYears(15)
                .hasDangerousGoodsCertificate(true)
                .dangerousGoodsCertificateExpiry(LocalDate.of(2025, 6, 1))
                .hasInternationalTransportationPermit(true)
                .hasOversizedCargoPermit(true)
                .hasRefrigeratedCargoPermit(true)
                .medicalCertificateNumber("МК-12345")
                .medicalCertificateExpiryDate(LocalDate.of(2024, 12, 31))
                .currentDrivingStatus(DrivingStatus.AVAILABILITY)
                .currentStatusStartTime(LocalDateTime.now().minusHours(2))
                .dailyDrivingMinutesToday(0)
                .continuousDrivingMinutes(0)
                .weeklyDrivingMinutes(0)
                .avgFuelEfficiencyPercent(95)
                .avgDeliveryTimeEfficiencyPercent(98)
                .hourlyRate(new BigDecimal("800.00"))
                .perKilometerRate(new BigDecimal("15.00"))
                .rating(new BigDecimal("4.8"))
                .completedRoutesCount(245)
                .totalDistanceDrivenKm(new BigDecimal("125000"))
                .incidentsCount(0)
                .workScheduleType("5/2")
                .weeklyWorkHours(40)
                .lastRestDay(LocalDate.now().minusDays(1))
                .knownRegions(Set.of("77", "78", "47", "50"))
                .adrClasses(Set.of("1", "3", "8"))
                .build(),

            Driver.builder()
                .firstName("Алексей")
                .lastName("Сидоров")
                .middleName("Владимирович")
                .birthDate(LocalDate.of(1978, 11, 8))
                .licenseNumber("6611224433")
                .licenseIssueDate(LocalDate.of(2008, 7, 10))
                .licenseExpiryDate(LocalDate.of(2028, 7, 10))
                .licenseCategories("B,C,CE,D")
                .phoneNumber("+7-901-987-65-43")
                .email("alexey.sidorov@example.com")
                .drivingExperienceYears(22)
                .hasDangerousGoodsCertificate(true)
                .hasInternationalTransportationPermit(true)
                .hasOversizedCargoPermit(false)
                .hasRefrigeratedCargoPermit(true)
                .medicalCertificateNumber("МК-67890")
                .medicalCertificateExpiryDate(LocalDate.of(2024, 8, 15))
                .currentDrivingStatus(DrivingStatus.AVAILABILITY)
                .currentStatusStartTime(LocalDateTime.now().minusHours(1))
                .dailyDrivingMinutesToday(0)
                .continuousDrivingMinutes(0)
                .weeklyDrivingMinutes(0)
                .avgFuelEfficiencyPercent(92)
                .avgDeliveryTimeEfficiencyPercent(95)
                .hourlyRate(new BigDecimal("950.00"))
                .perKilometerRate(new BigDecimal("18.00"))
                .rating(new BigDecimal("4.9"))
                .completedRoutesCount(312)
                .totalDistanceDrivenKm(new BigDecimal("180000"))
                .incidentsCount(1)
                .workScheduleType("2/2")
                .weeklyWorkHours(48)
                .lastRestDay(LocalDate.now().minusDays(2))
                .knownRegions(Set.of("77", "78", "23", "39"))
                .adrClasses(Set.of("2", "3", "4.1"))
                .build(),

            Driver.builder()
                .firstName("Михаил")
                .lastName("Кузнецов")
                .middleName("Андреевич")
                .birthDate(LocalDate.of(1990, 2, 22))
                .licenseNumber("5544332211")
                .licenseIssueDate(LocalDate.of(2015, 1, 12))
                .licenseExpiryDate(LocalDate.of(2035, 1, 12))
                .licenseCategories("B,C")
                .phoneNumber("+7-902-555-11-22")
                .email("mikhail.kuznetsov@example.com")
                .drivingExperienceYears(9)
                .hasDangerousGoodsCertificate(false)
                .hasInternationalTransportationPermit(false)
                .hasOversizedCargoPermit(false)
                .hasRefrigeratedCargoPermit(false)
                .medicalCertificateNumber("МК-11111")
                .medicalCertificateExpiryDate(LocalDate.of(2025, 3, 1))
                .currentDrivingStatus(DrivingStatus.AVAILABILITY)
                .currentStatusStartTime(LocalDateTime.now().minusMinutes(30))
                .dailyDrivingMinutesToday(0)
                .continuousDrivingMinutes(0)
                .weeklyDrivingMinutes(0)
                .avgFuelEfficiencyPercent(88)
                .avgDeliveryTimeEfficiencyPercent(90)
                .hourlyRate(new BigDecimal("650.00"))
                .perKilometerRate(new BigDecimal("12.00"))
                .rating(new BigDecimal("4.5"))
                .completedRoutesCount(89)
                .totalDistanceDrivenKm(new BigDecimal("45000"))
                .incidentsCount(0)
                .workScheduleType("5/2")
                .weeklyWorkHours(40)
                .lastRestDay(LocalDate.now().minusDays(1))
                .knownRegions(Set.of("77", "50", "33"))
                .adrClasses(Set.of())
                .build()
        );

        driverRepository.saveAll(drivers);
    }

    /**
     * Инициализирует тестовые транспортные средства
     */
    private void initVehicles() {
        log.info("🚚 Создание тестовых транспортных средств...");

        List<Vehicle> vehicles = List.of(
            Vehicle.builder()
                .registrationNumber("А123ВС77")
                .manufacturer("КАМАЗ")
                .model("65116")
                .productionYear(2020)
                .heightCm(320)
                .widthCm(250)
                .lengthCm(850)
                .emptyWeightKg(10000)
                .maxLoadCapacityKg(15000)
                .grossWeightKg(25000)
                .engineType("DIESEL")
                .fuelCapacityLitres(350)
                .fuelConsumptionPer100km(new BigDecimal("32.5"))
                .emissionClass("EURO_5")
                .axisConfiguration("6X4")
                .axisCount(3)
                .hasRefrigerator(false)
                .hasDangerousGoodsPermission(true)
                .hasOversizedCargoPermission(false)
                .currentFuelLevelLitres(new BigDecimal("280"))
                .currentOdometerKm(new BigDecimal("85000"))
                .lastMaintenanceDate(LocalDate.of(2024, 1, 15))
                .nextMaintenanceDate(LocalDate.of(2024, 7, 15))
                .maintenanceIntervalKm(10000)
                .avgSpeedKmh(65.0)
                .avgIdleTimePercent(15.0)
                .actualFuelConsumptionPer100km(new BigDecimal("34.2"))
                .adrCertificateNumber("ADR-123456")
                .adrCertificateExpiryDate(LocalDate.of(2025, 12, 31))
                .avgMaintenanceCostPerKm(new BigDecimal("2.50"))
                .vehicleDepreciationPerKm(new BigDecimal("8.00"))
                .build(),

            Vehicle.builder()
                .registrationNumber("В456ТР99")
                .manufacturer("МАН")
                .model("TGX 18.440")
                .productionYear(2019)
                .heightCm(400)
                .widthCm(255)
                .lengthCm(1650)
                .emptyWeightKg(12000)
                .maxLoadCapacityKg(18000)
                .grossWeightKg(40000)
                .engineType("DIESEL")
                .fuelCapacityLitres(400)
                .fuelConsumptionPer100km(new BigDecimal("28.0"))
                .emissionClass("EURO_6")
                .axisConfiguration("6X4")
                .axisCount(3)
                .hasRefrigerator(true)
                .hasDangerousGoodsPermission(false)
                .hasOversizedCargoPermission(false)
                .currentFuelLevelLitres(new BigDecimal("320"))
                .currentOdometerKm(new BigDecimal("120000"))
                .lastMaintenanceDate(LocalDate.of(2023, 12, 10))
                .nextMaintenanceDate(LocalDate.of(2024, 6, 10))
                .maintenanceIntervalKm(15000)
                .avgSpeedKmh(70.0)
                .avgIdleTimePercent(20.0)
                .actualFuelConsumptionPer100km(new BigDecimal("29.5"))
                .avgMaintenanceCostPerKm(new BigDecimal("3.20"))
                .vehicleDepreciationPerKm(new BigDecimal("12.00"))
                .build(),

            Vehicle.builder()
                .registrationNumber("Е789УК50")
                .manufacturer("Volvo")
                .model("FH16")
                .productionYear(2021)
                .heightCm(400)
                .widthCm(255)
                .lengthCm(1650)
                .emptyWeightKg(14000)
                .maxLoadCapacityKg(20000)
                .grossWeightKg(44000)
                .engineType("DIESEL")
                .fuelCapacityLitres(450)
                .fuelConsumptionPer100km(new BigDecimal("30.0"))
                .emissionClass("EURO_6")
                .axisConfiguration("8X4")
                .axisCount(4)
                .hasRefrigerator(false)
                .hasDangerousGoodsPermission(false)
                .hasOversizedCargoPermission(true)
                .currentFuelLevelLitres(new BigDecimal("380"))
                .currentOdometerKm(new BigDecimal("45000"))
                .lastMaintenanceDate(LocalDate.of(2024, 2, 1))
                .nextMaintenanceDate(LocalDate.of(2024, 8, 1))
                .maintenanceIntervalKm(20000)
                .avgSpeedKmh(68.0)
                .avgIdleTimePercent(12.0)
                .actualFuelConsumptionPer100km(new BigDecimal("31.2"))
                .avgMaintenanceCostPerKm(new BigDecimal("4.00"))
                .vehicleDepreciationPerKm(new BigDecimal("15.00"))
                .build()
        );

        vehicleRepository.saveAll(vehicles);
    }

    /**
     * Инициализирует тестовые грузы
     */
    private void initCargos() {
        log.info("📦 Создание тестовых грузов...");

        List<Cargo> cargos = List.of(
            Cargo.builder()
                .name("Строительные материалы")
                .description("Кирпич силикатный, поддоны")
                .weightKg(8500)
                .volumeCubicMeters(new BigDecimal("25.0"))
                .lengthCm(400)
                .widthCm(200)
                .heightCm(150)
                .cargoType(Cargo.CargoType.GENERAL)
                .isFragile(false)
                .isPerishable(false)
                .isDangerous(false)
                .requiresTemperatureControl(false)
                .requiresCustomsClearance(false)
                .declaredValue(new BigDecimal("450000"))
                .currency("RUB")
                .build(),

            Cargo.builder()
                .name("Замороженные продукты")
                .description("Мясная продукция, полуфабрикаты")
                .weightKg(12000)
                .volumeCubicMeters(new BigDecimal("35.0"))
                .lengthCm(600)
                .widthCm(240)
                .heightCm(200)
                .cargoType(Cargo.CargoType.REFRIGERATED)
                .isFragile(false)
                .isPerishable(true)
                .isDangerous(false)
                .requiresTemperatureControl(true)
                .minTemperatureCelsius(-18)
                .maxTemperatureCelsius(-15)
                .requiresCustomsClearance(false)
                .declaredValue(new BigDecimal("1200000"))
                .currency("RUB")
                .build(),

            Cargo.builder()
                .name("Бытовая техника")
                .description("Холодильники, стиральные машины")
                .weightKg(6500)
                .volumeCubicMeters(new BigDecimal("40.0"))
                .lengthCm(800)
                .widthCm(240)
                .heightCm(220)
                .cargoType(Cargo.CargoType.FRAGILE)
                .isFragile(true)
                .isPerishable(false)
                .isDangerous(false)
                .requiresTemperatureControl(false)
                .requiresCustomsClearance(false)
                .declaredValue(new BigDecimal("850000"))
                .currency("RUB")
                .build(),

            Cargo.builder()
                .name("Химические реактивы")
                .description("Промышленная химия класса 8")
                .weightKg(5000)
                .volumeCubicMeters(new BigDecimal("15.0"))
                .lengthCm(300)
                .widthCm(200)
                .heightCm(180)
                .cargoType(Cargo.CargoType.DANGEROUS)
                .isFragile(false)
                .isPerishable(false)
                .isDangerous(true)
                .dangerousGoodsClass("8")
                .unNumber("UN1760")
                .requiresTemperatureControl(false)
                .requiresCustomsClearance(true)
                .declaredValue(new BigDecimal("320000"))
                .currency("RUB")
                .build(),

            Cargo.builder()
                .name("Автомобильные запчасти")
                .description("Двигатели, КПП, запчасти")
                .weightKg(15000)
                .volumeCubicMeters(new BigDecimal("50.0"))
                .lengthCm(1000)
                .widthCm(240)
                .heightCm(250)
                .cargoType(Cargo.CargoType.HEAVY)
                .isFragile(false)
                .isPerishable(false)
                .isDangerous(false)
                .isOversized(true)
                .requiresTemperatureControl(false)
                .requiresCustomsClearance(false)
                .declaredValue(new BigDecimal("2500000"))
                .currency("RUB")
                .build()
        );

        cargoRepository.saveAll(cargos);
    }

    /**
     * Инициализирует тестовые маршруты
     */
    private void initRoutes() {
        log.info("🗺️ Создание тестовых маршрутов...");

        try {
            List<Route> routes = List.of(
                Route.builder()
                    .name("Москва - Санкт-Петербург")
                    .startAddress("Москва, МКАД 47км")
                    .startLat(55.7558)
                    .startLon(37.6176)
                    .endAddress("Санкт-Петербург, КАД 23км")
                    .endLat(59.9311)
                    .endLon(30.3609)
                    .distanceKm(new BigDecimal("635"))
                    .estimatedDurationMinutes(480)
                    .estimatedFuelConsumption(new BigDecimal("206.75"))
                    .estimatedFuelCost(new BigDecimal("11337"))
                    .estimatedTollCost(new BigDecimal("1200"))
                    .estimatedDriverCost(new BigDecimal("6400"))
                    .estimatedTotalCost(new BigDecimal("18937"))
                    .currency("RUB")
                    .overallRiskScore(new BigDecimal("32"))
                    .weatherRiskScore(new BigDecimal("25"))
                    .roadQualityRiskScore(new BigDecimal("15"))
                    .trafficRiskScore(new BigDecimal("40"))
                    .cargoRiskScore(new BigDecimal("20"))
                    .status(Route.RouteStatus.PLANNED)
                    .departureTime(LocalDateTime.now().plusHours(2))
                    .estimatedArrivalTime(LocalDateTime.now().plusHours(10))
                    .build(),

                Route.builder()
                    .name("Москва - Казань")
                    .startAddress("Москва, Щелковское шоссе")
                    .startLat(55.7558)
                    .startLon(37.6176)
                    .endAddress("Казань, автовокзал")
                    .endLat(55.8304)
                    .endLon(49.0661)
                    .distanceKm(new BigDecimal("815"))
                    .estimatedDurationMinutes(600)
                    .estimatedFuelConsumption(new BigDecimal("264.88"))
                    .estimatedFuelCost(new BigDecimal("14518"))
                    .estimatedTollCost(new BigDecimal("800"))
                    .estimatedDriverCost(new BigDecimal("8000"))
                    .estimatedTotalCost(new BigDecimal("23318"))
                    .currency("RUB")
                    .overallRiskScore(new BigDecimal("28"))
                    .weatherRiskScore(new BigDecimal("30"))
                    .roadQualityRiskScore(new BigDecimal("20"))
                    .trafficRiskScore(new BigDecimal("25"))
                    .cargoRiskScore(new BigDecimal("35"))
                    .status(Route.RouteStatus.PLANNED)
                    .departureTime(LocalDateTime.now().plusDays(1))
                    .estimatedArrivalTime(LocalDateTime.now().plusDays(1).plusHours(10))
                    .build(),

                Route.builder()
                    .name("Санкт-Петербург - Новгород")
                    .startAddress("Санкт-Петербург, КАД 12км")
                    .startLat(59.9311)
                    .startLon(30.3609)
                    .endAddress("Великий Новгород, ТЦ Русь")
                    .endLat(58.5219)
                    .endLon(31.2756)
                    .distanceKm(new BigDecimal("190"))
                    .estimatedDurationMinutes(150)
                    .estimatedFuelConsumption(new BigDecimal("61.75"))
                    .estimatedFuelCost(new BigDecimal("3386"))
                    .estimatedTollCost(new BigDecimal("0"))
                    .estimatedDriverCost(new BigDecimal("2000"))
                    .estimatedTotalCost(new BigDecimal("5386"))
                    .currency("RUB")
                    .overallRiskScore(new BigDecimal("21"))
                    .weatherRiskScore(new BigDecimal("20"))
                    .roadQualityRiskScore(new BigDecimal("25"))
                    .trafficRiskScore(new BigDecimal("15"))
                    .cargoRiskScore(new BigDecimal("25"))
                    .status(Route.RouteStatus.PLANNED)
                    .departureTime(LocalDateTime.now().plusDays(2))
                    .estimatedArrivalTime(LocalDateTime.now().plusDays(2).plusHours(3))
                    .build()
            );

            routeRepository.saveAll(routes);
        } catch (Exception e) {
            log.warn("⚠️ Не удалось создать тестовые маршруты: {}", e.getMessage());
            // Продолжаем выполнение, маршруты не критичны для базовой функциональности
        }
    }

    /**
     * Выводит инструкции для тестирования через Swagger
     */
    private void logTestingInstructions() {
        log.info("📖 ========== ИНСТРУКЦИИ ПО ТЕСТИРОВАНИЮ ==========");
        log.info("🌐 Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("🔐 Учетные данные для авторизации:");
        log.info("   admin/admin123 - полные права");
        log.info("   dispatcher/disp123 - управление операциями");
        log.info("   manager/manager123 - аналитика");
        log.info("   driver/driver123 - водитель");
        log.info("   supervisor/super123 - супервайзер");
        log.info("🧪 Тестовые данные созданы:");
        log.info("   Пользователи: {}", userRepository.count());
        log.info("   Водители: {}", driverRepository.count());
        log.info("   ТС: {}", vehicleRepository.count());
        log.info("   Грузы: {}", cargoRepository.count());
        log.info("   Маршруты: {}", routeRepository.count());
        log.info("🗺️  Основные endpoint'ы для тестирования:");
        log.info("   POST /api/routes/calculate - расчет с полной аналитикой");
        log.info("   GET /api/routes/plan - планирование по координатам");
        log.info("   GET /api/routes/plan-by-name - планирование по названиям");
        log.info("   GET /api/drivers - список водителей");
        log.info("   GET /api/vehicles - список ТС");
        log.info("   GET /api/cargos - список грузов");
        log.info("   GET /api/routes - список маршрутов");
        log.info("📋 Пример запроса для /api/routes/calculate:");
        log.info("   {{\"vehicleId\": 1, \"driverId\": 1, \"cargoId\": 1,");
        log.info("     \"startLat\": 55.7558, \"startLon\": 37.6176,");
        log.info("     \"endLat\": 59.9311, \"endLon\": 30.3609}}");
        log.info("==================================================");
    }
} 

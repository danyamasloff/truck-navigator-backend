package ru.maslov.trucknavigator.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.maslov.trucknavigator.entity.*;
import ru.maslov.trucknavigator.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Компонент для инициализации тестовых данных при запуске приложения в dev-профиле.
 * Реализует CommandLineRunner для выполнения кода после загрузки контекста приложения.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CargoRepository cargoRepository;
    private final UserRepository userRepository;
    private final RouteRepository routeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Начало инициализации тестовых данных...");

            boolean dataExists = userRepository.count() > 0 ||
                    vehicleRepository.count() > 0 ||
                    driverRepository.count() > 0 ||
                    cargoRepository.count() > 0 ||
                    routeRepository.count() > 0;

            if (dataExists) {
                log.info("Обнаружены существующие данные. Выполняется выборочная инициализация...");
            }

            if (userRepository.count() == 0) {
                initUsers();
            }

            if (vehicleRepository.count() == 0) {
                initVehicles();
            }

            if (driverRepository.count() == 0) {
                initDrivers();
            }

            if (cargoRepository.count() == 0) {
                initCargos();
            }

            if (routeRepository.count() == 0) {
                initRoutes();
            }

            log.info("Инициализация тестовых данных успешно завершена");
        } catch (Exception e) {
            log.error("Ошибка при инициализации тестовых данных: {}", e.getMessage(), e);
            throw e; // Пробрасываем исключение, чтобы Spring мог его обработать
        }
    }

    /**
     * Инициализирует тестовых пользователей с различными ролями
     */
    private void initUsers() {
        log.info("Создание тестовых пользователей...");

        // Администратор
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@truck-navigator.ru")
                .firstName("Администратор")
                .lastName("Системы")
                .active(true)
                .roles(Set.of("ROLE_ADMIN"))
                .createdAt(LocalDateTime.now())
                .build();

        // Диспетчер
        User dispatcher = User.builder()
                .username("dispatcher")
                .password(passwordEncoder.encode("disp123"))
                .email("dispatcher@truck-navigator.ru")
                .firstName("Диспетчер")
                .lastName("Смирнов")
                .active(true)
                .roles(Set.of("ROLE_DISPATCHER"))
                .createdAt(LocalDateTime.now())
                .build();

        // Водитель
        User driver = User.builder()
                .username("driver")
                .password(passwordEncoder.encode("driver123"))
                .email("driver@truck-navigator.ru")
                .firstName("Иван")
                .lastName("Водителев")
                .active(true)
                .roles(Set.of("ROLE_DRIVER"))
                .createdAt(LocalDateTime.now())
                .build();

        // Пользователь с несколькими ролями
        User multiRole = User.builder()
                .username("manager")
                .password(passwordEncoder.encode("manager123"))
                .email("manager@truck-navigator.ru")
                .firstName("Менеджер")
                .lastName("Руководителев")
                .active(true)
                .roles(Set.of("ROLE_DISPATCHER", "ROLE_MANAGER"))
                .createdAt(LocalDateTime.now())
                .build();

        // Сохраняем всех пользователей
        userRepository.saveAll(List.of(admin, dispatcher, driver, multiRole));

        log.info("Создано {} пользователей", 4);
    }

    /**
     * Инициализирует тестовые транспортные средства
     */
    private void initVehicles() {
        log.info("Создание тестовых транспортных средств...");

        // КамАЗ тягач
        Vehicle truck1 = Vehicle.builder()
                .registrationNumber("А123БВ777")
                .model("КАМАЗ-5490")
                .manufacturer("КАМАЗ")
                .productionYear(2020)
                .heightCm(380)
                .widthCm(255)
                .lengthCm(1680)
                .emptyWeightKg(7900)
                .maxLoadCapacityKg(10500)
                .grossWeightKg(18400)
                .engineType("DIESEL")
                .fuelCapacityLitres(400)
                .fuelConsumptionPer100km(new BigDecimal("33.5"))
                .emissionClass("EURO_5")
                .axisConfiguration("4X2")
                .axisCount(2)
                .hasRefrigerator(false)
                .hasDangerousGoodsPermission(false)
                .hasOversizedCargoPermission(false)
                .currentFuelLevelLitres(new BigDecimal("320.0"))
                .currentOdometerKm(new BigDecimal("125000.0"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Volvo с рефрижератором
        Vehicle truck2 = Vehicle.builder()
                .registrationNumber("Е456ВК178")
                .model("Volvo FH")
                .manufacturer("Volvo")
                .productionYear(2021)
                .heightCm(390)
                .widthCm(255)
                .lengthCm(1650)
                .emptyWeightKg(9000)
                .maxLoadCapacityKg(11000)
                .grossWeightKg(20000)
                .engineType("DIESEL")
                .fuelCapacityLitres(600)
                .fuelConsumptionPer100km(new BigDecimal("30.8"))
                .emissionClass("EURO_6")
                .axisConfiguration("6X4")
                .axisCount(3)
                .hasRefrigerator(true)
                .hasDangerousGoodsPermission(true)
                .hasOversizedCargoPermission(false)
                .currentFuelLevelLitres(new BigDecimal("450.0"))
                .currentOdometerKm(new BigDecimal("87200.0"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // MAN для негабаритных грузов
        Vehicle truck3 = Vehicle.builder()
                .registrationNumber("О789МН750")
                .model("MAN TGX")
                .manufacturer("MAN")
                .productionYear(2019)
                .heightCm(370)
                .widthCm(255)
                .lengthCm(1640)
                .emptyWeightKg(8500)
                .maxLoadCapacityKg(10000)
                .grossWeightKg(18500)
                .engineType("DIESEL")
                .fuelCapacityLitres(500)
                .fuelConsumptionPer100km(new BigDecimal("31.2"))
                .emissionClass("EURO_5")
                .axisConfiguration("4X2")
                .axisCount(2)
                .hasRefrigerator(false)
                .hasDangerousGoodsPermission(false)
                .hasOversizedCargoPermission(true)
                .currentFuelLevelLitres(new BigDecimal("280.0"))
                .currentOdometerKm(new BigDecimal("210500.0"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Scania для опасных грузов
        Vehicle truck4 = Vehicle.builder()
                .registrationNumber("Т555УФ116")
                .model("Scania R500")
                .manufacturer("Scania")
                .productionYear(2022)
                .heightCm(385)
                .widthCm(250)
                .lengthCm(1620)
                .emptyWeightKg(8700)
                .maxLoadCapacityKg(12000)
                .grossWeightKg(20700)
                .engineType("DIESEL")
                .fuelCapacityLitres(550)
                .fuelConsumptionPer100km(new BigDecimal("29.5"))
                .emissionClass("EURO_6")
                .axisConfiguration("6X4")
                .axisCount(3)
                .hasRefrigerator(false)
                .hasDangerousGoodsPermission(true)
                .hasOversizedCargoPermission(false)
                .currentFuelLevelLitres(new BigDecimal("410.0"))
                .currentOdometerKm(new BigDecimal("45600.0"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Сохраняем все ТС
        vehicleRepository.saveAll(List.of(truck1, truck2, truck3, truck4));

        log.info("Создано {} транспортных средств", 4);
    }

    /**
     * Инициализирует тестовых водителей
     */
    private void initDrivers() {
        log.info("Создание тестовых водителей...");

        // Водитель с базовыми правами
        Driver driver1 = Driver.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .middleName("Иванович")
                .birthDate(LocalDate.of(1985, 5, 15))
                .licenseNumber("7777 123456")
                .licenseIssueDate(LocalDate.of(2015, 3, 10))
                .licenseExpiryDate(LocalDate.of(2025, 3, 10))
                .licenseCategories("B, C, CE")
                .phoneNumber("+7 (999) 123-45-67")
                .email("ivanov@example.com")
                .drivingExperienceYears(10)
                .hasDangerousGoodsCertificate(false)
                .hasInternationalTransportationPermit(false)
                .hourlyRate(new BigDecimal("300.00"))
                .perKilometerRate(new BigDecimal("10.00"))
                .currentDrivingStatus(Driver.DrivingStatus.AVAILABILITY)
                .currentStatusStartTime(LocalDateTime.now().minusHours(1))
                .dailyDrivingMinutesToday(180)
                .continuousDrivingMinutes(0)
                .weeklyDrivingMinutes(1200)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Опытный водитель с международными перевозками
        Driver driver2 = Driver.builder()
                .firstName("Петр")
                .lastName("Петров")
                .middleName("Петрович")
                .birthDate(LocalDate.of(1980, 10, 25))
                .licenseNumber("5555 987654")
                .licenseIssueDate(LocalDate.of(2010, 7, 20))
                .licenseExpiryDate(LocalDate.of(2030, 7, 20))
                .licenseCategories("B, C, CE, D")
                .phoneNumber("+7 (999) 765-43-21")
                .email("petrov@example.com")
                .drivingExperienceYears(15)
                .hasDangerousGoodsCertificate(true)
                .dangerousGoodsCertificateExpiry(LocalDate.of(2024, 12, 31))
                .hasInternationalTransportationPermit(true)
                .hourlyRate(new BigDecimal("350.00"))
                .perKilometerRate(new BigDecimal("12.00"))
                .currentDrivingStatus(Driver.DrivingStatus.DRIVING)
                .currentStatusStartTime(LocalDateTime.now().minusMinutes(90))
                .dailyDrivingMinutesToday(240)
                .continuousDrivingMinutes(90)
                .weeklyDrivingMinutes(1500)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Водитель на отдыхе
        Driver driver3 = Driver.builder()
                .firstName("Сергей")
                .lastName("Сергеев")
                .middleName("Сергеевич")
                .birthDate(LocalDate.of(1990, 2, 8))
                .licenseNumber("6666 456789")
                .licenseIssueDate(LocalDate.of(2018, 5, 15))
                .licenseExpiryDate(LocalDate.of(2028, 5, 15))
                .licenseCategories("B, C, CE")
                .phoneNumber("+7 (999) 555-55-55")
                .email("sergeev@example.com")
                .drivingExperienceYears(5)
                .hasDangerousGoodsCertificate(false)
                .hasInternationalTransportationPermit(false)
                .hourlyRate(new BigDecimal("280.00"))
                .perKilometerRate(new BigDecimal("9.00"))
                .currentDrivingStatus(Driver.DrivingStatus.DAILY_REST)
                .currentStatusStartTime(LocalDateTime.now().minusHours(5))
                .dailyDrivingMinutesToday(360)
                .continuousDrivingMinutes(0)
                .weeklyDrivingMinutes(1800)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Водитель со сверхурочными часами
        Driver driver4 = Driver.builder()
                .firstName("Алексей")
                .lastName("Алексеев")
                .middleName("Алексеевич")
                .birthDate(LocalDate.of(1988, 7, 12))
                .licenseNumber("1234 567890")
                .licenseIssueDate(LocalDate.of(2012, 8, 25))
                .licenseExpiryDate(LocalDate.of(2027, 8, 25))
                .licenseCategories("B, C, CE")
                .phoneNumber("+7 (999) 111-22-33")
                .email("alekseev@example.com")
                .drivingExperienceYears(8)
                .hasDangerousGoodsCertificate(true)
                .dangerousGoodsCertificateExpiry(LocalDate.of(2025, 6, 15))
                .hasInternationalTransportationPermit(true)
                .hourlyRate(new BigDecimal("320.00"))
                .perKilometerRate(new BigDecimal("11.00"))
                .currentDrivingStatus(Driver.DrivingStatus.DRIVING)
                .currentStatusStartTime(LocalDateTime.now().minusHours(4))
                .dailyDrivingMinutesToday(400)
                .continuousDrivingMinutes(240)
                .weeklyDrivingMinutes(2500)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Сохраняем всех водителей
        driverRepository.saveAll(List.of(driver1, driver2, driver3, driver4));

        log.info("Создано {} водителей", 4);
    }

    /**
     * Инициализирует тестовые грузы
     */
    private void initCargos() {
        log.info("Создание тестовых грузов...");

        // Бытовая техника (генеральный груз)
        Cargo cargo1 = Cargo.builder()
                .name("Бытовая техника")
                .description("Холодильники, стиральные машины, телевизоры")
                .weightKg(5000)
                .volumeCubicMeters(new BigDecimal("30.0"))
                .lengthCm(600)
                .widthCm(240)
                .heightCm(220)
                .cargoType(Cargo.CargoType.GENERAL)
                .isFragile(true)
                .isPerishable(false)
                .isDangerous(false)
                .isOversized(false)
                .requiresTemperatureControl(false)
                .requiresCustomsClearance(false)
                .declaredValue(new BigDecimal("2000000.00"))
                .currency("RUB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Замороженные продукты (рефрижераторный груз)
        Cargo cargo2 = Cargo.builder()
                .name("Продукты питания")
                .description("Замороженные полуфабрикаты, мясо, рыба")
                .weightKg(8000)
                .volumeCubicMeters(new BigDecimal("25.0"))
                .lengthCm(550)
                .widthCm(240)
                .heightCm(220)
                .cargoType(Cargo.CargoType.REFRIGERATED)
                .isFragile(false)
                .isPerishable(true)
                .isDangerous(false)
                .isOversized(false)
                .requiresTemperatureControl(true)
                .minTemperatureCelsius(-18)
                .maxTemperatureCelsius(-10)
                .requiresCustomsClearance(false)
                .declaredValue(new BigDecimal("1500000.00"))
                .currency("RUB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Опасный груз
        Cargo cargo3 = Cargo.builder()
                .name("Химическая продукция")
                .description("Неорганические кислоты, класс опасности 8")
                .weightKg(6000)
                .volumeCubicMeters(new BigDecimal("20.0"))
                .lengthCm(500)
                .widthCm(240)
                .heightCm(200)
                .cargoType(Cargo.CargoType.DANGEROUS)
                .isFragile(false)
                .isPerishable(false)
                .isDangerous(true)
                .dangerousGoodsClass("8")
                .unNumber("UN1830")
                .isOversized(false)
                .requiresTemperatureControl(false)
                .requiresCustomsClearance(true)
                .declaredValue(new BigDecimal("1200000.00"))
                .currency("RUB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Негабаритный груз
        Cargo cargo4 = Cargo.builder()
                .name("Строительное оборудование")
                .description("Бетономешалка, экскаватор малый")
                .weightKg(12000)
                .volumeCubicMeters(new BigDecimal("40.0"))
                .lengthCm(750)
                .widthCm(260)
                .heightCm(280)
                .cargoType(Cargo.CargoType.OVERSIZED)
                .isFragile(false)
                .isPerishable(false)
                .isDangerous(false)
                .isOversized(true)
                .requiresTemperatureControl(false)
                .requiresCustomsClearance(false)
                .declaredValue(new BigDecimal("5000000.00"))
                .currency("RUB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Ценный груз
        Cargo cargo5 = Cargo.builder()
                .name("Электроника")
                .description("Смартфоны, ноутбуки, комплектующие")
                .weightKg(3000)
                .volumeCubicMeters(new BigDecimal("15.0"))
                .lengthCm(400)
                .widthCm(220)
                .heightCm(180)
                .cargoType(Cargo.CargoType.VALUABLE)
                .isFragile(true)
                .isPerishable(false)
                .isDangerous(false)
                .isOversized(false)
                .requiresTemperatureControl(false)
                .requiresCustomsClearance(true)
                .declaredValue(new BigDecimal("8000000.00"))
                .currency("RUB")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Сохраняем все грузы
        cargoRepository.saveAll(List.of(cargo1, cargo2, cargo3, cargo4, cargo5));

        log.info("Создано {} грузов", 5);
    }

    /**
     * Инициализирует тестовые маршруты с промежуточными точками
     */
    private void initRoutes() {
        log.info("Создание тестовых маршрутов...");

        // Получаем сохраненные сущности
        List<Vehicle> vehicles = vehicleRepository.findAll();
        List<Driver> drivers = driverRepository.findAll();
        List<Cargo> cargos = cargoRepository.findAll();

        if (vehicles.isEmpty() || drivers.isEmpty() || cargos.isEmpty()) {
            log.warn("Невозможно создать маршруты: отсутствуют ТС, водители или грузы");
            return;
        }

        // Маршрут 1: Москва - Санкт-Петербург
        Route route1 = Route.builder()
                .name("Москва - Санкт-Петербург")
                .startAddress("Москва, Складочная ул., 1")
                .startLat(55.8003)
                .startLon(37.5917)
                .endAddress("Санкт-Петербург, Софийская ул., 145")
                .endLat(59.8325)
                .endLon(30.3991)
                .distanceKm(new BigDecimal("705.5"))
                .estimatedDurationMinutes(510) // 8.5 часов
                .vehicle(vehicles.get(0))
                .driver(drivers.get(0))
                .cargo(cargos.get(0))
                .departureTime(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0))
                .estimatedArrivalTime(LocalDateTime.now().plusDays(1).withHour(16).withMinute(30))
                .estimatedFuelConsumption(new BigDecimal("235.2"))
                .estimatedFuelCost(new BigDecimal("15288.00"))
                .estimatedTollCost(new BigDecimal("1300.00"))
                .estimatedDriverCost(new BigDecimal("7055.00"))
                .estimatedTotalCost(new BigDecimal("23643.00"))
                .weatherRiskScore(new BigDecimal("15.5"))
                .roadQualityRiskScore(new BigDecimal("20.0"))
                .trafficRiskScore(new BigDecimal("30.0"))
                .overallRiskScore(new BigDecimal("22.8"))
                .status(Route.RouteStatus.PLANNED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Добавляем промежуточные точки для маршрута 1
        List<Waypoint> waypoints1 = Arrays.asList(
                Waypoint.builder()
                        .route(route1)
                        .orderIndex(1)
                        .name("Заправка и отдых")
                        .address("Тверь, АЗС Лукойл на M-10")
                        .latitude(56.8584)
                        .longitude(35.9043)
                        .waypointType(Waypoint.WaypointType.FUEL)
                        .plannedArrivalTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(30))
                        .plannedDepartureTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0))
                        .stayDurationMinutes(30)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                Waypoint.builder()
                        .route(route1)
                        .orderIndex(2)
                        .name("Обеденный перерыв")
                        .address("Великий Новгород, ул. Ломоносова, 20")
                        .latitude(58.5223)
                        .longitude(31.2831)
                        .waypointType(Waypoint.WaypointType.REST)
                        .plannedArrivalTime(LocalDateTime.now().plusDays(1).withHour(13).withMinute(30))
                        .plannedDepartureTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(15))
                        .stayDurationMinutes(45)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        route1.setWaypoints(waypoints1);

        // Маршрут 2: Санкт-Петербург - Калининград
        Route route2 = Route.builder()
                .name("Санкт-Петербург - Калининград")
                .startAddress("Санкт-Петербург, Пулковское шоссе, 42")
                .startLat(59.7908)
                .startLon(30.3335)
                .endAddress("Калининград, Индустриальная ул., 4")
                .endLat(54.7064)
                .endLon(20.5121)
                .distanceKm(new BigDecimal("952.3"))
                .estimatedDurationMinutes(720) // 12 часов
                .vehicle(vehicles.get(1))
                .driver(drivers.get(1))
                .cargo(cargos.get(1))
                .departureTime(LocalDateTime.now().plusDays(2).withHour(6).withMinute(0))
                .estimatedArrivalTime(LocalDateTime.now().plusDays(2).withHour(18).withMinute(0))
                .estimatedFuelConsumption(new BigDecimal("293.3"))
                .estimatedFuelCost(new BigDecimal("19064.5"))
                .estimatedTollCost(new BigDecimal("2400.00"))
                .estimatedDriverCost(new BigDecimal("11427.6"))
                .estimatedTotalCost(new BigDecimal("32892.1"))
                .weatherRiskScore(new BigDecimal("35.2"))
                .roadQualityRiskScore(new BigDecimal("25.5"))
                .trafficRiskScore(new BigDecimal("15.0"))
                .overallRiskScore(new BigDecimal("26.2"))
                .status(Route.RouteStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Промежуточные точки для маршрута 2
        List<Waypoint> waypoints2 = Arrays.asList(
                Waypoint.builder()
                        .route(route2)
                        .orderIndex(1)
                        .name("Таможенный пункт")
                        .address("Граница РФ-Латвия, п. Шумилкино")
                        .latitude(57.8735)
                        .longitude(27.3388)
                        .waypointType(Waypoint.WaypointType.CUSTOMS)
                        .plannedArrivalTime(LocalDateTime.now().plusDays(2).withHour(10).withMinute(0))
                        .plannedDepartureTime(LocalDateTime.now().plusDays(2).withHour(11).withMinute(30))
                        .stayDurationMinutes(90)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                Waypoint.builder()
                        .route(route2)
                        .orderIndex(2)
                        .name("Обязательный отдых")
                        .address("Рига, Логистический центр")
                        .latitude(56.9460)
                        .longitude(24.1059)
                        .waypointType(Waypoint.WaypointType.REST)
                        .plannedArrivalTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0))
                        .plannedDepartureTime(LocalDateTime.now().plusDays(2).withHour(15).withMinute(0))
                        .stayDurationMinutes(60)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                Waypoint.builder()
                        .route(route2)
                        .orderIndex(3)
                        .name("Пересечение границы ЕС - Калининградская область")
                        .address("Пограничный переход Советск")
                        .latitude(55.0852)
                        .longitude(21.8834)
                        .waypointType(Waypoint.WaypointType.CUSTOMS)
                        .plannedArrivalTime(LocalDateTime.now().plusDays(2).withHour(16).withMinute(30))
                        .plannedDepartureTime(LocalDateTime.now().plusDays(2).withHour(17).withMinute(15))
                        .stayDurationMinutes(45)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        route2.setWaypoints(waypoints2);

        // Маршрут 3: Казань - Новосибирск (длинный маршрут с негабаритным грузом)
        Route route3 = Route.builder()
                .name("Казань - Новосибирск (Негабаритный груз)")
                .startAddress("Казань, ул. Техническая, 17")
                .startLat(55.7671)
                .startLon(49.0967)
                .endAddress("Новосибирск, ул. Петухова, 55")
                .endLat(55.0415)
                .endLon(82.8981)
                .distanceKm(new BigDecimal("2384.6"))
                .estimatedDurationMinutes(2160) // 36 часов (3 дня по 12 часов)
                .vehicle(vehicles.get(2))
                .driver(drivers.get(2))
                .cargo(cargos.get(3)) // негабаритный груз
                .departureTime(LocalDateTime.now().plusDays(3).withHour(6).withMinute(0))
                .estimatedArrivalTime(LocalDateTime.now().plusDays(6).withHour(18).withMinute(0))
                .estimatedFuelConsumption(new BigDecimal("743.9"))
                .estimatedFuelCost(new BigDecimal("48353.5"))
                .estimatedTollCost(new BigDecimal("0.00")) // нет платных дорог
                .estimatedDriverCost(new BigDecimal("21461.4"))
                .estimatedTotalCost(new BigDecimal("69814.9"))
                .weatherRiskScore(new BigDecimal("55.3"))
                .roadQualityRiskScore(new BigDecimal("65.7"))
                .trafficRiskScore(new BigDecimal("20.0"))
                .overallRiskScore(new BigDecimal("48.7"))
                .status(Route.RouteStatus.PLANNED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Маршрут 4: Москва - Ростов-на-Дону (опасный груз)
        Route route4 = Route.builder()
                .name("Москва - Ростов-на-Дону (Опасный груз)")
                .startAddress("Москва, МКАД, 84-й километр")
                .startLat(55.9129)
                .startLon(37.5430)
                .endAddress("Ростов-на-Дону, ул. Доватора, 154")
                .endLat(47.2724)
                .endLon(39.6913)
                .distanceKm(new BigDecimal("1057.8"))
                .estimatedDurationMinutes(840) // 14 часов
                .vehicle(vehicles.get(3))
                .driver(drivers.get(1)) // опытный водитель с допуском на опасные грузы
                .cargo(cargos.get(2)) // опасный груз
                .departureTime(LocalDateTime.now().plusDays(4).withHour(5).withMinute(0))
                .estimatedArrivalTime(LocalDateTime.now().plusDays(4).withHour(19).withMinute(0))
                .estimatedFuelConsumption(new BigDecimal("312.1"))
                .estimatedFuelCost(new BigDecimal("20286.5"))
                .estimatedTollCost(new BigDecimal("1850.00"))
                .estimatedDriverCost(new BigDecimal("12693.6"))
                .estimatedTotalCost(new BigDecimal("34830.1"))
                .weatherRiskScore(new BigDecimal("25.0"))
                .roadQualityRiskScore(new BigDecimal("30.0"))
                .trafficRiskScore(new BigDecimal("35.0"))
                .overallRiskScore(new BigDecimal("30.0"))
                .status(Route.RouteStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Сохраняем все маршруты вместе с промежуточными точками
        List<Route> routes = routeRepository.saveAll(List.of(route1, route2, route3, route4));

        log.info("Создано {} маршрутов", routes.size());
    }
}
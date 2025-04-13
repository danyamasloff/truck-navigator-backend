package ru.maslov.trucknavigator.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.maslov.trucknavigator.entity.Cargo;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.entity.User;
import ru.maslov.trucknavigator.entity.Vehicle;
import ru.maslov.trucknavigator.repository.CargoRepository;
import ru.maslov.trucknavigator.repository.DriverRepository;
import ru.maslov.trucknavigator.repository.UserRepository;
import ru.maslov.trucknavigator.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Класс для инициализации тестовых данных при запуске приложения в dev-профиле.
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CargoRepository cargoRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Инициализирует тестовые данные при запуске приложения.
     */
    @Bean
    public CommandLineRunner initData() {
        return args -> {
            log.info("Инициализация тестовых данных...");

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

            log.info("Инициализация тестовых данных завершена");
        };
    }

    /**
     * Инициализирует тестовые данные пользователей.
     */
    private void initUsers() {
        log.info("Создание тестовых пользователей...");

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@truck-navigator.ru")
                .firstName("Администратор")
                .lastName("Системы")
                .active(true)
                .roles(Set.of("ROLE_ADMIN"))
                .build();

        User dispatcher = User.builder()
                .username("dispatcher")
                .password(passwordEncoder.encode("disp123"))
                .email("dispatcher@truck-navigator.ru")
                .firstName("Диспетчер")
                .lastName("Смирнов")
                .active(true)
                .roles(Set.of("ROLE_DISPATCHER"))
                .build();

        User driver = User.builder()
                .username("driver")
                .password(passwordEncoder.encode("driver123"))
                .email("driver@truck-navigator.ru")
                .firstName("Иван")
                .lastName("Водителев")
                .active(true)
                .roles(Set.of("ROLE_DRIVER"))
                .build();

        userRepository.saveAll(List.of(admin, dispatcher, driver));

        log.info("Создано {} пользователей", 3);
    }

    /**
     * Инициализирует тестовые данные транспортных средств.
     */
    private void initVehicles() {
        log.info("Создание тестовых транспортных средств...");

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

        vehicleRepository.saveAll(List.of(truck1, truck2, truck3));

        log.info("Создано {} транспортных средств", 3);
    }

    /**
     * Инициализирует тестовые данные водителей.
     */
    private void initDrivers() {
        log.info("Создание тестовых водителей...");

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

        driverRepository.saveAll(List.of(driver1, driver2, driver3));

        log.info("Создано {} водителей", 3);
    }

    /**
     * Инициализирует тестовые данные грузов.
     */
    private void initCargos() {
        log.info("Создание тестовых грузов...");

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

        cargoRepository.saveAll(List.of(cargo1, cargo2, cargo3, cargo4));

        log.info("Создано {} грузов", 4);
    }
}
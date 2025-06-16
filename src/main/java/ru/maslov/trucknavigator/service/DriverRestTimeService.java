package ru.maslov.trucknavigator.service;
import ru.maslov.trucknavigator.entity.DrivingStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.dto.driver.DriverRestAnalysisDto;
import ru.maslov.trucknavigator.dto.driver.RestStopRecommendationDto;
import ru.maslov.trucknavigator.dto.geocoding.GeoLocationDto;
import ru.maslov.trucknavigator.dto.routing.RouteResponseDto;
import ru.maslov.trucknavigator.entity.Driver;
import ru.maslov.trucknavigator.integration.graphhopper.GeocodingService;
import ru.maslov.trucknavigator.integration.fuelprice.FuelPriceService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Сервис для контроля режима труда и отдыха водителя согласно нормативам.
 * Реализует логику проверки соответствия РТО законодательству РФ (Приказ №424)
 * и формирует рекомендации по планированию остановок для отдыха.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DriverRestTimeService {

    // Нормативы РТО из конфигурации
    @Value("${driver.rest.short.break.minutes:15}")
    private int shortBreakMinutes;

    @Value("${driver.rest.long.break.minutes:45}")
    private int longBreakMinutes;

    @Value("${driver.work.max.continuous.minutes:270}")
    private int maxContinuousDrivingMinutes;

    @Value("${driver.work.max.daily.minutes:540}")
    private int maxDailyDrivingMinutes;

    // Радиусы поиска для различных типов мест (в метрах)
    @Value("${driver.rest.search.radius.short:5000}")
    private int shortBreakSearchRadius;

    @Value("${driver.rest.search.radius.long:10000}")
    private int longBreakSearchRadius;

    @Value("${driver.rest.search.radius.daily:20000}")
    private int dailyBreakSearchRadius;

    // Максимальное количество найденных мест для анализа
    @Value("${driver.rest.max.locations:5}")
    private int maxLocationsToAnalyze;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Зависимости от других сервисов
    private final GeocodingService geocodingService;
    private final FuelPriceService fuelPriceService;

    // Пул потоков для асинхронных операций, управляемый Spring
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    /**
     * Анализирует соответствие режима труда и отдыха водителя нормативам
     * на основе планируемого маршрута.
     *
     * @param driver информация о водителе
     * @param route маршрут
     * @param departureTime время отправления
     * @return результат анализа РТО с рекомендациями
     */
    public DriverRestAnalysisDto analyzeDriverRestTime(Driver driver, RouteResponseDto route, LocalDateTime departureTime) {
        if (driver == null || route == null || departureTime == null) {
            log.warn("Невозможно выполнить анализ РТО: недостаточно данных");
            return null;
        }

        DriverRestAnalysisDto analysis = new DriverRestAnalysisDto();
        analysis.setDriverId(driver.getId());
        analysis.setDriverName(driver.getLastName() + " " + driver.getFirstName());
        analysis.setDepartureTime(departureTime);

        // Расчет времени в пути и прибытия
        long durationInMinutes = route.getDuration();
        LocalDateTime arrivalTime = departureTime.plusMinutes(durationInMinutes);
        analysis.setEstimatedArrivalTime(arrivalTime);
        analysis.setEstimatedTripDurationMinutes((int) durationInMinutes);

        // Проверка текущего статуса вождения
        analysis.setCurrentDrivingStatus(driver.getCurrentDrivingStatus());
        analysis.setCurrentStatusStartTime(driver.getCurrentStatusStartTime());

        // Расчет остаточного времени вождения
        int remainingContinuousDrivingMinutes = calculateRemainingContinuousDrivingTime(driver);
        int remainingDailyDrivingMinutes = calculateRemainingDailyDrivingTime(driver);
        analysis.setRemainingContinuousDrivingMinutes(remainingContinuousDrivingMinutes);
        analysis.setRemainingDailyDrivingMinutes(remainingDailyDrivingMinutes);

        // Генерация рекомендаций по остановкам для отдыха
        List<RestStopRecommendationDto> recommendations = generateRestStopRecommendations(
                driver, route, departureTime, remainingContinuousDrivingMinutes, remainingDailyDrivingMinutes);
        analysis.setRestStopRecommendations(recommendations);

        // Проверка соответствия нормативам
        boolean compliant = checkComplianceWithRegulations(durationInMinutes,
                remainingContinuousDrivingMinutes, remainingDailyDrivingMinutes,
                recommendations.isEmpty());
        analysis.setCompliantWithRegulations(compliant);

        // Создание сводной информации
        analysis.setSummary(generateSummary(analysis));

        return analysis;
    }

    /**
     * Рассчитывает оставшееся время непрерывного вождения для водителя.
     *
     * @param driver информация о водителе
     * @return оставшееся время в минутах
     */
    private int calculateRemainingContinuousDrivingTime(Driver driver) {
        // Если водитель в режиме вождения, вычитаем уже проведенное время
        if (DrivingStatus.DRIVING.equals(driver.getCurrentDrivingStatus())
                && driver.getCurrentStatusStartTime() != null
                && driver.getContinuousDrivingMinutes() != null) {

            return maxContinuousDrivingMinutes - driver.getContinuousDrivingMinutes();
        }

        // Если водитель не за рулем или нет данных, возвращаем полное доступное время
        return maxContinuousDrivingMinutes;
    }

    /**
     * Рассчитывает оставшееся время суточного вождения для водителя.
     *
     * @param driver информация о водителе
     * @return оставшееся время в минутах
     */
    private int calculateRemainingDailyDrivingTime(Driver driver) {
        // Проверяем, сколько времени водитель уже отработал сегодня
        if (driver.getDailyDrivingMinutesToday() != null) {
            return maxDailyDrivingMinutes - driver.getDailyDrivingMinutesToday();
        }

        // Если нет данных, возвращаем полное доступное время
        return maxDailyDrivingMinutes;
    }

    /**
     * Генерирует рекомендации по остановкам для отдыха во время маршрута.
     *
     * @param driver информация о водителе
     * @param route маршрут
     * @param departureTime время отправления
     * @param remainingContinuousDrivingMinutes оставшееся время непрерывного вождения
     * @param remainingDailyDrivingMinutes оставшееся время суточного вождения
     * @return список рекомендаций по остановкам
     */
    private List<RestStopRecommendationDto> generateRestStopRecommendations(
            Driver driver, RouteResponseDto route, LocalDateTime departureTime,
            int remainingContinuousDrivingMinutes, int remainingDailyDrivingMinutes) {

        List<RestStopRecommendationDto> recommendations = new ArrayList<>();
        long totalDurationMinutes = route.getDuration();

        // Если общее время поездки меньше оставшегося времени непрерывного вождения,
        // остановки не требуются
        if (totalDurationMinutes <= remainingContinuousDrivingMinutes &&
                totalDurationMinutes <= remainingDailyDrivingMinutes) {
            return recommendations;
        }

        // Текущее время в пути
        int elapsedMinutes = 0;

        // Оставшееся время вождения
        int remainingContinuous = remainingContinuousDrivingMinutes;
        int remainingDaily = remainingDailyDrivingMinutes;

        // Текущее время (для расчета времени остановок)
        LocalDateTime currentTime = departureTime;

        // Карта асинхронных задач обогащения рекомендаций (индекс -> задача)
        Map<Integer, CompletableFuture<RestStopRecommendationDto>> enrichmentTasksMap = new HashMap<>();

        // Формируем рекомендации, пока не дойдем до конца маршрута
        while (elapsedMinutes < totalDurationMinutes) {
            int nextDrivingSegment;
            int restDuration;
            String restType;
            String reason;

            // Определяем, какое ограничение сработает раньше
            if (remainingContinuous <= remainingDaily) {
                // Ограничение по непрерывному вождению
                nextDrivingSegment = Math.min(remainingContinuous, (int)(totalDurationMinutes - elapsedMinutes));
                restDuration = longBreakMinutes;
                restType = "Длительный отдых";
                reason = "Достигнут лимит непрерывного вождения";

                // После длительного отдыха сбрасываем счетчик непрерывного вождения
                remainingContinuous = maxContinuousDrivingMinutes;
            } else {
                // Ограничение по суточному вождению
                nextDrivingSegment = Math.min(remainingDaily, (int)(totalDurationMinutes - elapsedMinutes));

                if (nextDrivingSegment >= remainingContinuous) {
                    // Но сначала нужен перерыв по непрерывному вождению
                    nextDrivingSegment = remainingContinuous;
                    restDuration = longBreakMinutes;
                    restType = "Длительный отдых";
                    reason = "Достигнут лимит непрерывного вождения";

                    // После отдыха сбрасываем счетчик непрерывного вождения
                    remainingContinuous = maxContinuousDrivingMinutes;
                } else {
                    // Достигнут суточный лимит
                    restDuration = 660; // 11 часов (минимальный суточный отдых)
                    restType = "Суточный отдых";
                    reason = "Достигнут суточный лимит вождения";

                    // После суточного отдыха сбрасываем оба счетчика
                    remainingContinuous = maxContinuousDrivingMinutes;
                    remainingDaily = maxDailyDrivingMinutes;
                }
            }

            // Перемещаемся вперед по маршруту
            elapsedMinutes += nextDrivingSegment;
            currentTime = currentTime.plusMinutes(nextDrivingSegment);

            // Уменьшаем оставшееся время вождения
            remainingContinuous -= nextDrivingSegment;
            remainingDaily -= nextDrivingSegment;

            // Если мы еще не достигли конца маршрута, добавляем рекомендацию по остановке
            if (elapsedMinutes < totalDurationMinutes) {
                // Создаем базовую рекомендацию
                RestStopRecommendationDto recommendation = RestStopRecommendationDto.builder()
                        .distanceFromStartKm(calculateDistanceAtTime(route, elapsedMinutes))
                        .timeFromDeparture(formatDuration(Duration.ofMinutes(elapsedMinutes)))
                        .expectedArrivalAtStop(currentTime)
                        .recommendedRestDurationMinutes(restDuration)
                        .restType(restType)
                        .reason(reason)
                        .facilities(new HashMap<>()) // Инициализируем пустую карту
                        .build();

                // Добавляем координаты точки остановки (если возможно)
                if (route.getCoordinates() != null && !route.getCoordinates().isEmpty()) {
                    double[] coords = findCoordinatesAtDistance(route, recommendation.getDistanceFromStartKm());
                    if (coords != null) {
                        recommendation.setLongitude(coords[0]);
                        recommendation.setLatitude(coords[1]);

                        // Сохраняем индекс для привязки к задаче
                        final int recommendationIndex = recommendations.size();

                        // Создаем асинхронную задачу обогащения рекомендации
                        log.debug("Создание задачи обогащения для рекомендации {}: {},{}",
                                recommendationIndex, recommendation.getLatitude(), recommendation.getLongitude());

                        CompletableFuture<RestStopRecommendationDto> enrichmentTask =
                                CompletableFuture.supplyAsync(() ->
                                                enhanceRecommendationWithRealLocation(recommendation, restType),
                                        taskExecutor);

                        // Сохраняем задачу с привязкой к индексу рекомендации
                        enrichmentTasksMap.put(recommendationIndex, enrichmentTask);
                    }
                }

                recommendations.add(recommendation);

                // Учитываем время отдыха
                currentTime = currentTime.plusMinutes(restDuration);
            }
        }

        // Ждем завершения всех асинхронных задач обогащения и обновляем рекомендации
        if (!enrichmentTasksMap.isEmpty()) {
            try {
                // Ждем завершения всех задач
                CompletableFuture.allOf(enrichmentTasksMap.values().toArray(new CompletableFuture[0])).join();

                // Обновляем рекомендации согласно результатам обогащения
                for (Map.Entry<Integer, CompletableFuture<RestStopRecommendationDto>> entry : enrichmentTasksMap.entrySet()) {
                    int index = entry.getKey();
                    try {
                        RestStopRecommendationDto enrichedRecommendation = entry.getValue().get();

                        // Проверяем, успешно ли прошло обогащение
                        if (enrichedRecommendation != null &&
                                enrichedRecommendation.getLocationName() != null &&
                                !enrichedRecommendation.getLocationName().isEmpty() &&
                                index < recommendations.size()) {

                            log.debug("Обновление рекомендации {}: {} -> {}",
                                    index,
                                    recommendations.get(index).getLocationName(),
                                    enrichedRecommendation.getLocationName());

                            recommendations.set(index, enrichedRecommendation);
                        }
                    } catch (Exception e) {
                        log.error("Ошибка при получении результатов обогащения для рекомендации {}: {}",
                                index, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка при обработке результатов обогащения рекомендаций: {}", e.getMessage(), e);
            }
        }

        return recommendations;
    }

    /**
     * Обогащает рекомендацию по остановке информацией о реальном месте остановки.
     *
     * @param baseRecommendation базовая рекомендация
     * @param restType тип отдыха
     * @return обогащенная рекомендация
     */
    private RestStopRecommendationDto enhanceRecommendationWithRealLocation(
            RestStopRecommendationDto baseRecommendation, String restType) {

        try {
            // Защитная проверка входных параметров
            if (baseRecommendation == null) {
                log.warn("enhanceRecommendationWithRealLocation: получена null рекомендация");
                return null;
            }

            if (baseRecommendation.getLatitude() == null || baseRecommendation.getLongitude() == null) {
                log.warn("enhanceRecommendationWithRealLocation: координаты не определены");
                return baseRecommendation;
            }

            log.debug("Обогащение рекомендации для координат {},{} типа {}",
                    baseRecommendation.getLatitude(),
                    baseRecommendation.getLongitude(),
                    restType);

            // Определяем радиус поиска и тип мест в зависимости от типа отдыха
            int searchRadius;
            List<GeoLocationDto> locations = new ArrayList<>();

            switch(restType) {
                case "Длительный отдых":
                    searchRadius = longBreakSearchRadius;
                    // Для длительного отдыха ищем места для отдыха и парковки
                    try {
                        locations.addAll(geocodingService.findRestAreas(
                                baseRecommendation.getLatitude(),
                                baseRecommendation.getLongitude(),
                                searchRadius));
                    } catch (Exception e) {
                        log.warn("Ошибка при поиске зон отдыха: {}", e.getMessage());
                    }

                    // Если мест для отдыха недостаточно, добавляем парковки
                    if (locations.size() < 2) {
                        try {
                            locations.addAll(geocodingService.findParkingSpots(
                                    baseRecommendation.getLatitude(),
                                    baseRecommendation.getLongitude(),
                                    searchRadius));
                        } catch (Exception e) {
                            log.warn("Ошибка при поиске парковок: {}", e.getMessage());
                        }
                    }

                    // Добавляем заправки (они тоже подходят для длительного отдыха)
                    try {
                        locations.addAll(geocodingService.findFuelStations(
                                baseRecommendation.getLatitude(),
                                baseRecommendation.getLongitude(),
                                searchRadius));
                    } catch (Exception e) {
                        log.warn("Ошибка при поиске заправок: {}", e.getMessage());
                    }
                    break;

                case "Суточный отдых":
                    searchRadius = dailyBreakSearchRadius;
                    // Для суточного отдыха ищем места для ночлега
                    try {
                        locations.addAll(geocodingService.findLodging(
                                baseRecommendation.getLatitude(),
                                baseRecommendation.getLongitude(),
                                searchRadius));
                    } catch (Exception e) {
                        log.warn("Ошибка при поиске мест для ночлега: {}", e.getMessage());
                    }

                    // Если отелей нет, ищем места для отдыха
                    if (locations.isEmpty()) {
                        try {
                            locations.addAll(geocodingService.findRestAreas(
                                    baseRecommendation.getLatitude(),
                                    baseRecommendation.getLongitude(),
                                    searchRadius));
                        } catch (Exception e) {
                            log.warn("Ошибка при поиске зон отдыха: {}", e.getMessage());
                        }
                    }
                    break;

                default: // Короткий перерыв
                    searchRadius = shortBreakSearchRadius;
                    // Для короткого перерыва ищем парковки и заправки
                    try {
                        locations.addAll(geocodingService.findParkingSpots(
                                baseRecommendation.getLatitude(),
                                baseRecommendation.getLongitude(),
                                searchRadius));
                    } catch (Exception e) {
                        log.warn("Ошибка при поиске парковок: {}", e.getMessage());
                    }

                    try {
                        locations.addAll(geocodingService.findFuelStations(
                                baseRecommendation.getLatitude(),
                                baseRecommendation.getLongitude(),
                                searchRadius));
                    } catch (Exception e) {
                        log.warn("Ошибка при поиске заправок: {}", e.getMessage());
                    }
                    break;
            }

            // Если найдены подходящие места
            if (!locations.isEmpty()) {
                log.debug("Найдено {} потенциальных мест для остановки", locations.size());

                // Ограничиваем количество мест для анализа
                List<GeoLocationDto> filteredLocations = locations.stream()
                        .filter(loc -> loc != null) // Фильтруем null
                        .distinct()
                        .limit(maxLocationsToAnalyze)
                        .collect(Collectors.toList());

                // Выбираем лучшее место
                GeoLocationDto bestLocation = selectBestLocation(filteredLocations);

                if (bestLocation != null) {
                    log.debug("Выбрано место для остановки: {}", bestLocation.getName());

                    // Обновляем координаты и добавляем информацию о реальном месте
                    baseRecommendation.setLongitude(bestLocation.getLongitude());
                    baseRecommendation.setLatitude(bestLocation.getLatitude());
                    baseRecommendation.setLocationName(bestLocation.getName());

                    // Безопасно добавляем описание, проверяя на null
                    if (bestLocation.getDescription() != null) {
                        baseRecommendation.setLocationDescription(bestLocation.getDescription());
                    }

                    // Безопасно добавляем тип, проверяя на null
                    if (bestLocation.getType() != null) {
                        baseRecommendation.setLocationType(bestLocation.getType());
                    }

                    // Расстояние от маршрута
                    baseRecommendation.setDistanceFromRoute(
                            calculateDistance(
                                    baseRecommendation.getLatitude(),
                                    baseRecommendation.getLongitude(),
                                    bestLocation.getLatitude(),
                                    bestLocation.getLongitude()
                            )
                    );

                    // Заполняем данные о доступных услугах в зависимости от типа места
                    baseRecommendation.setFacilities(determineFacilities(bestLocation));

                    // Добавляем данные о стоимости парковки (заглушка, в реальном приложении - из API)
                    String locationType = bestLocation.getType();
                    if (locationType != null && "parking".equals(locationType)) {
                        baseRecommendation.setParkingCost(0.0); // Бесплатно по умолчанию
                    }

                    // Добавляем данные о цене топлива, если это заправка
                    if (locationType != null && "fuel".equals(locationType)) {
                        try {
                            Double fuelPrice = fuelPriceService.getFuelPrice(
                                    bestLocation.getLatitude(),
                                    bestLocation.getLongitude(),
                                    "DIESEL" // По умолчанию дизель
                            ).getPrice().doubleValue();

                            baseRecommendation.setFuelPrice(fuelPrice);
                        } catch (Exception e) {
                            log.warn("Не удалось получить цену на топливо: {}", e.getMessage());
                        }
                    }
                }
            } else {
                log.debug("Не найдено подходящих мест для остановки в радиусе {} м", searchRadius);
            }
        } catch (Exception e) {
            log.error("Ошибка при обогащении рекомендации: {}", e.getMessage(), e);
        }

        return baseRecommendation;
    }

    /**
     * Определяет доступные услуги для места остановки.
     *
     * @param location информация о месте
     * @return карта доступных услуг
     */
    private Map<String, Boolean> determineFacilities(GeoLocationDto location) {
        Map<String, Boolean> facilities = new HashMap<>();

        // Защитная проверка для избежания NPE
        if (location == null) {
            // Базовый набор услуг по умолчанию
            facilities.put("toilet", false);
            facilities.put("parking", false);
            facilities.put("fuel", false);
            facilities.put("shop", false);
            facilities.put("food", false);
            facilities.put("lodging", false);
            facilities.put("shower", false);
            facilities.put("wifi", false);
            return facilities;
        }

        // Базовые сервисы
        facilities.put("toilet", true); // Считаем, что туалет есть везде
        facilities.put("parking", true); // Считаем, что парковка есть везде

        // Получаем безопасно тип и описание, избегая NPE
        String type = location.getType() != null ? location.getType().toLowerCase() : "";
        String description = location.getDescription() != null ?
                location.getDescription().toLowerCase() : "";

        // Дополнительные сервисы по типу места
        if ("fuel".equals(type)) {
            facilities.put("fuel", true);
            facilities.put("shop", true);
        } else {
            facilities.put("fuel", false);
            facilities.put("shop", false);
        }

        if ("restaurant".equals(type) || "cafe".equals(type) || description.contains("кафе")) {
            facilities.put("food", true);
        } else {
            facilities.put("food", false);
        }

        if ("hotel".equals(type) || "motel".equals(type) || "tourism".equals(type)) {
            facilities.put("lodging", true);
            facilities.put("shower", true);
        } else {
            facilities.put("lodging", false);
            facilities.put("shower", false);
        }

        // Определение по ключевым словам в описании
        if (description.contains("душ") || description.contains("shower")) {
            facilities.put("shower", true);
        }

        if (description.contains("wifi") || description.contains("интернет")) {
            facilities.put("wifi", true);
        } else {
            facilities.put("wifi", false);
        }

        return facilities;
    }

    /**
     * Выбирает лучшее место для остановки из списка доступных мест.
     *
     * @param locations список доступных мест
     * @return лучшее место для остановки
     */
    private GeoLocationDto selectBestLocation(List<GeoLocationDto> locations) {
        if (locations == null || locations.isEmpty()) {
            return null;
        }

        // Проверка на null элементы
        locations = locations.stream()
                .filter(loc -> loc != null)
                .collect(Collectors.toList());

        if (locations.isEmpty()) {
            return null;
        }

        // Приоритеты типов мест (от высшего к низшему)
        Map<String, Integer> typePriority = new HashMap<>();
        typePriority.put("rest_area", 10);
        typePriority.put("fuel", 8);
        typePriority.put("parking", 6);
        typePriority.put("hotel", 5);
        typePriority.put("tourism", 5);  // Тип для отелей, которые возвращает GeocodingService
        typePriority.put("restaurant", 4);
        typePriority.put("cafe", 3);

        // Сортируем по приоритету типа (в первую очередь) и описанию (во вторую)
        return locations.stream()
                .sorted(Comparator
                        .comparing((GeoLocationDto l) -> {
                            String type = l.getType();
                            return typePriority.getOrDefault(
                                    type != null ? type.toLowerCase() : "", 0);
                        })
                        .reversed()
                        .thenComparing(l -> l.getDescription() != null ?
                                l.getDescription() : ""))
                .findFirst()
                .orElse(locations.get(0));
    }

    /**
     * Рассчитывает пройденное расстояние на указанной временной отметке маршрута.
     *
     * @param route маршрут
     * @param elapsedMinutes прошедшее время в минутах
     * @return пройденное расстояние в километрах
     */
    private double calculateDistanceAtTime(RouteResponseDto route, int elapsedMinutes) {
        // Предполагаем, что скорость постоянна на всем маршруте
        double fractionOfTotalTime = (double) elapsedMinutes / route.getDuration();
        return route.getDistance().doubleValue() * fractionOfTotalTime;
    }

    /**
     * Находит координаты точки на маршруте, соответствующей указанному расстоянию от начала.
     *
     * @param route маршрут
     * @param distanceKm расстояние от начала в километрах
     * @return координаты точки [долгота, широта] или null, если невозможно определить
     */
    private double[] findCoordinatesAtDistance(RouteResponseDto route, double distanceKm) {
        // Если нет координат, вернуть null
        if (route.getCoordinates() == null || route.getCoordinates().isEmpty()) {
            return null;
        }

        // Проверка на валидность расстояния и наличия информации о дистанции
        if (distanceKm < 0 || route.getDistance() == null || route.getDistance().doubleValue() <= 0) {
            log.warn("Некорректные данные для поиска координат: дистанция={}, totalDist={}",
                    distanceKm, route.getDistance());
            return route.getCoordinates().get(0); // Возвращаем начальную точку при проблемах
        }

        // Расчет индекса точки на основе пропорции
        double fractionOfTotalDistance = distanceKm / route.getDistance().doubleValue();
        int index = (int) (fractionOfTotalDistance * (route.getCoordinates().size() - 1));

        // Проверка на выход за границы массива
        if (index < 0) {
            index = 0;
        } else if (index >= route.getCoordinates().size()) {
            index = route.getCoordinates().size() - 1;
        }

        return route.getCoordinates().get(index);
    }

    /**
     * Проверяет соответствие маршрута нормативам РТО.
     *
     * @param totalDurationMinutes общая длительность маршрута в минутах
     * @param remainingContinuousDrivingMinutes оставшееся время непрерывного вождения
     * @param remainingDailyDrivingMinutes оставшееся время суточного вождения
     * @param hasNoRecommendations флаг отсутствия рекомендаций по остановкам
     * @return true, если маршрут соответствует нормативам
     */
    private boolean checkComplianceWithRegulations(long totalDurationMinutes,
                                                   int remainingContinuousDrivingMinutes,
                                                   int remainingDailyDrivingMinutes,
                                                   boolean hasNoRecommendations) {
        // Если нет рекомендаций по остановкам, значит маршрут соответствует нормативам
        if (hasNoRecommendations) {
            return true;
        }

        // Проверяем, есть ли превышение лимитов без учета остановок
        return !(totalDurationMinutes > remainingContinuousDrivingMinutes &&
                totalDurationMinutes > remainingDailyDrivingMinutes);
    }

    /**
     * Формирует текстовое резюме анализа РТО.
     *
     * @param analysis результат анализа РТО
     * @return текстовое описание
     */
    private String generateSummary(DriverRestAnalysisDto analysis) {
        StringBuilder summary = new StringBuilder();

        // Базовая информация
        summary.append(String.format("Водитель: %s\n", analysis.getDriverName()));
        summary.append(String.format("Отправление: %s\n",
                analysis.getDepartureTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        summary.append(String.format("Прибытие: %s\n",
                analysis.getEstimatedArrivalTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))));
        summary.append(String.format("Время в пути: %s\n",
                formatDuration(Duration.ofMinutes(analysis.getEstimatedTripDurationMinutes()))));

        // Текущий статус и оставшееся время
        summary.append(String.format("Текущий статус: %s\n",
                formatDrivingStatus(analysis.getCurrentDrivingStatus())));

        summary.append(String.format("Оставшееся время непрерывного вождения: %d мин.\n",
                analysis.getRemainingContinuousDrivingMinutes()));
        summary.append(String.format("Оставшееся время суточного вождения: %d мин.\n",
                analysis.getRemainingDailyDrivingMinutes()));

        // Соответствие нормативам
        if (analysis.isCompliantWithRegulations()) {
            if (analysis.getRestStopRecommendations() == null || analysis.getRestStopRecommendations().isEmpty()) {
                summary.append("Маршрут соответствует нормативам РТО без необходимости остановок для отдыха.\n");
            } else {
                summary.append("Маршрут соответствует нормативам РТО при соблюдении рекомендованных остановок для отдыха.\n");
            }
        } else {
            summary.append("ВНИМАНИЕ! Маршрут не соответствует нормативам РТО. Требуется корректировка.\n");
        }

        // Рекомендации по остановкам
        if (analysis.getRestStopRecommendations() != null && !analysis.getRestStopRecommendations().isEmpty()) {
            summary.append("\nРекомендуемые остановки для отдыха:\n");

            for (int i = 0; i < analysis.getRestStopRecommendations().size(); i++) {
                RestStopRecommendationDto stop = analysis.getRestStopRecommendations().get(i);

                // Базовая информация об остановке
                StringBuilder stopInfo = new StringBuilder(String.format("%d. Через %s пути (%.1f км) - %s на %d мин. - %s",
                        i + 1,
                        stop.getTimeFromDeparture(),
                        stop.getDistanceFromStartKm(),
                        stop.getRestType(),
                        stop.getRecommendedRestDurationMinutes(),
                        stop.getReason()));

                // Добавляем информацию о реальном месте остановки, если есть
                if (stop.getLocationName() != null && !stop.getLocationName().isEmpty()) {
                    stopInfo.append(String.format("\n   Место: %s", stop.getLocationName()));

                    if (stop.getLocationDescription() != null && !stop.getLocationDescription().isEmpty()) {
                        stopInfo.append(String.format(" (%s)", stop.getLocationDescription()));
                    }

                    // Информация о доступных услугах
                    if (stop.getFacilities() != null && !stop.getFacilities().isEmpty()) {
                        List<String> availableServices = new ArrayList<>();
                        for (Map.Entry<String, Boolean> entry : stop.getFacilities().entrySet()) {
                            if (entry.getValue() != null && entry.getValue()) {
                                availableServices.add(translateFacility(entry.getKey()));
                            }
                        }

                        if (!availableServices.isEmpty()) {
                            stopInfo.append("\n   Услуги: ");
                            stopInfo.append(String.join(", ", availableServices));
                        }
                    }

                    // Информация о ценах
                    if (stop.getFuelPrice() != null) {
                        stopInfo.append(String.format("\n   Цена топлива: %.2f руб./л", stop.getFuelPrice()));
                    }

                    if (stop.getParkingCost() != null && stop.getParkingCost() > 0) {
                        stopInfo.append(String.format("\n   Стоимость парковки: %.2f руб.", stop.getParkingCost()));
                    }
                }

                summary.append(stopInfo).append("\n\n");
            }
        }

        return summary.toString();
    }

    /**
     * Переводит названия услуг на русский язык для отображения.
     */
    private String translateFacility(String facilityKey) {
        if (facilityKey == null) {
            return "";
        }

        switch(facilityKey) {
            case "toilet": return "туалет";
            case "shower": return "душ";
            case "food": return "питание";
            case "fuel": return "заправка";
            case "shop": return "магазин";
            case "wifi": return "Wi-Fi";
            case "lodging": return "ночлег";
            case "parking": return "парковка";
            default: return facilityKey;
        }
    }

    /**
     * Форматирует продолжительность в удобочитаемый формат.
     *
     * @param duration продолжительность
     * @return строковое представление в формате "Xч Yмин"
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%dч %dмин", hours, minutes);
        } else {
            return String.format("%dмин", minutes);
        }
    }

    /**
     * Форматирует статус вождения в удобочитаемый формат.
     *
     * @param status статус вождения
     * @return строковое представление статуса
     */
    private String formatDrivingStatus(DrivingStatus status) {
        if (status == null) {
            return "Неизвестно";
        }

        switch (status) {
            case DRIVING:
                return "Вождение";
            case REST_BREAK:
                return "Короткий перерыв";
            case DAILY_REST:
                return "Ежедневный отдых";
            case WEEKLY_REST:
                return "Еженедельный отдых";
            case OTHER_WORK:
                return "Другая работа";
            case AVAILABILITY:
                return "В режиме готовности";
            default:
                return status.toString();
        }
    }

    /**
     * Обновляет статус вождения водителя.
     *
     * @param driver водитель
     * @param newStatus новый статус
     * @param statusChangeTime время изменения статуса
     * @return обновленный объект водителя
     */
    public Driver updateDriverStatus(Driver driver, DrivingStatus newStatus, LocalDateTime statusChangeTime) {
        if (driver == null || newStatus == null || statusChangeTime == null) {
            return driver;
        }

        // Сохраняем старый статус для расчетов
        DrivingStatus oldStatus = driver.getCurrentDrivingStatus();
        LocalDateTime oldStatusStartTime = driver.getCurrentStatusStartTime();

        // Если статус не меняется, просто возвращаем водителя
        if (newStatus == oldStatus) {
            return driver;
        }

        // Если был статус и время старта, обновляем счетчики
        if (oldStatus != null && oldStatusStartTime != null) {
            long minutesInPreviousStatus = Duration.between(oldStatusStartTime, statusChangeTime).toMinutes();

            // Обновляем счетчики в зависимости от старого статуса
            if (DrivingStatus.DRIVING.equals(oldStatus)) {
                // Увеличиваем счетчик непрерывного вождения
                int continuousDriving = driver.getContinuousDrivingMinutes() != null ?
                        driver.getContinuousDrivingMinutes() : 0;
                driver.setContinuousDrivingMinutes(continuousDriving + (int) minutesInPreviousStatus);

                // Увеличиваем счетчик суточного вождения
                int dailyDriving = driver.getDailyDrivingMinutesToday() != null ?
                        driver.getDailyDrivingMinutesToday() : 0;
                driver.setDailyDrivingMinutesToday(dailyDriving + (int) minutesInPreviousStatus);

                // Увеличиваем счетчик недельного вождения
                int weeklyDriving = driver.getWeeklyDrivingMinutes() != null ?
                        driver.getWeeklyDrivingMinutes() : 0;
                driver.setWeeklyDrivingMinutes(weeklyDriving + (int) minutesInPreviousStatus);
            }
        }

        // Устанавливаем новый статус и время
        driver.setCurrentDrivingStatus(newStatus);
        driver.setCurrentStatusStartTime(statusChangeTime);

        // Если новый статус - отдых, сбрасываем счетчик непрерывного вождения
        if (DrivingStatus.REST_BREAK.equals(newStatus) ||
                DrivingStatus.DAILY_REST.equals(newStatus) ||
                DrivingStatus.WEEKLY_REST.equals(newStatus)) {

            // Длительный отдых сбрасывает счетчик непрерывного вождения
            if (oldStatus != null && oldStatusStartTime != null) {
                long restMinutes = Duration.between(oldStatusStartTime, statusChangeTime).toMinutes();
                if (restMinutes >= longBreakMinutes) {
                    driver.setContinuousDrivingMinutes(0);
                }
            }

            // Суточный отдых сбрасывает счетчик суточного вождения
            if (DrivingStatus.DAILY_REST.equals(newStatus) ||
                    DrivingStatus.WEEKLY_REST.equals(newStatus)) {
                driver.setDailyDrivingMinutesToday(0);
            }

            // Недельный отдых сбрасывает счетчик недельного вождения
            if (DrivingStatus.WEEKLY_REST.equals(newStatus)) {
                driver.setWeeklyDrivingMinutes(0);
            }
        }

        return driver;
    }

    /**
     * Рассчитывает расстояние между двумя географическими точками (по формуле гаверсинуса).
     *
     * @param lat1 широта первой точки
     * @param lon1 долгота первой точки
     * @param lat2 широта второй точки
     * @param lon2 долгота второй точки
     * @return расстояние в метрах
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Радиус Земли в км

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // Расстояние в метрах
    }
}

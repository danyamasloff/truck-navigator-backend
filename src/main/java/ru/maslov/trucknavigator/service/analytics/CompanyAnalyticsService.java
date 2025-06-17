package ru.maslov.trucknavigator.service.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maslov.trucknavigator.entity.Route;
import ru.maslov.trucknavigator.service.RouteService;
import ru.maslov.trucknavigator.service.DriverService;
import ru.maslov.trucknavigator.service.VehicleService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyAnalyticsService {

    private final RouteService routeService;
    private final DriverService driverService;
    private final VehicleService vehicleService;

    public Map<String, Object> getCompanyAnalytics(String period, String startDate, String endDate) {
        log.info("Получение общей аналитики компании за период: {}", period);

        Map<String, Object> analytics = new HashMap<>();
        
        // Получаем все маршруты (в реальной системе здесь была бы фильтрация по датам)
        List<Route> routes = routeService.findAll();
        
        // Рассчитываем KPI
        analytics.put("revenue", calculateRevenue(routes));
        analytics.put("costs", calculateCosts(routes));
        analytics.put("profit", calculateProfit(routes));
        analytics.put("efficiency", calculateEfficiency());
        analytics.put("monthlyData", generateMonthlyData());

        return analytics;
    }

    public Map<String, Object> getDriversAnalytics(String period, Integer limit) {
        log.info("Получение аналитики по водителям за период: {}, лимит: {}", period, limit);

        Map<String, Object> analytics = new HashMap<>();
        
        // Генерируем данные по водителям (в реальной системе - из БД)
        analytics.put("drivers", generateDriversPerformance(limit));

        return analytics;
    }

    public Map<String, Object> getRoutesAnalytics(String period, Integer limit) {
        log.info("Получение аналитики по маршрутам за период: {}, лимит: {}", period, limit);

        Map<String, Object> analytics = new HashMap<>();
        
        // Получаем популярные маршруты
        analytics.put("routes", generatePopularRoutes(limit));

        return analytics;
    }

    private Map<String, Object> calculateRevenue(List<Route> routes) {
        double current = routes.stream()
                .mapToDouble(route -> route.getEstimatedTotalCost() != null ? 
                    route.getEstimatedTotalCost().doubleValue() * 1.3 : 0) // С маржой
                .sum();
        
        double previous = current * 0.85; // Симуляция предыдущего периода
        double change = ((current - previous) / previous) * 100;

        Map<String, Object> revenue = new HashMap<>();
        revenue.put("current", current);
        revenue.put("previous", previous);
        revenue.put("change", Math.round(change * 10.0) / 10.0);
        
        return revenue;
    }

    private Map<String, Object> calculateCosts(List<Route> routes) {
        double current = routes.stream()
                .mapToDouble(route -> route.getEstimatedTotalCost() != null ? 
                    route.getEstimatedTotalCost().doubleValue() : 0)
                .sum();
        
        double previous = current * 0.9; // Симуляция предыдущего периода
        double change = ((current - previous) / previous) * 100;

        Map<String, Object> costs = new HashMap<>();
        costs.put("current", current);
        costs.put("previous", previous);
        costs.put("change", Math.round(change * 10.0) / 10.0);
        
        return costs;
    }

    private Map<String, Object> calculateProfit(List<Route> routes) {
        double revenue = routes.stream()
                .mapToDouble(route -> route.getEstimatedTotalCost() != null ? 
                    route.getEstimatedTotalCost().doubleValue() * 1.3 : 0)
                .sum();
        
        double costs = routes.stream()
                .mapToDouble(route -> route.getEstimatedTotalCost() != null ? 
                    route.getEstimatedTotalCost().doubleValue() : 0)
                .sum();
        
        double current = revenue - costs;
        double previous = current * 0.8; // Симуляция предыдущего периода
        double change = ((current - previous) / previous) * 100;

        Map<String, Object> profit = new HashMap<>();
        profit.put("current", current);
        profit.put("previous", previous);
        profit.put("change", Math.round(change * 10.0) / 10.0);
        
        return profit;
    }

    private Map<String, Object> calculateEfficiency() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        Map<String, Object> efficiency = new HashMap<>();
        efficiency.put("fuelConsumption", 15.5 + random.nextDouble() * 5); // 15.5-20.5 л/100км
        efficiency.put("onTimeDeliveries", 90 + random.nextDouble() * 10); // 90-100%
        efficiency.put("vehicleUtilization", 70 + random.nextDouble() * 25); // 70-95%
        efficiency.put("driverUtilization", 60 + random.nextDouble() * 30); // 60-90%
        
        return efficiency;
    }

    private List<Map<String, Object>> generateMonthlyData() {
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        String[] months = {"Янв", "Фев", "Мар", "Апр", "Май", "Июн"};
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (String month : months) {
            Map<String, Object> data = new HashMap<>();
            data.put("month", month);
            data.put("revenue", 1000000 + random.nextInt(500000));
            data.put("costs", 600000 + random.nextInt(300000));
            data.put("routes", 30 + random.nextInt(20));
            data.put("fuelConsumption", 8000 + random.nextInt(2000));
            monthlyData.add(data);
        }

        return monthlyData;
    }

    private List<Map<String, Object>> generateDriversPerformance(Integer limit) {
        List<Map<String, Object>> drivers = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Получаем реальных водителей или генерируем тестовые данные
        try {
            var realDrivers = driverService.findAll();
            int count = Math.min(limit, realDrivers.size());
            
            for (int i = 0; i < count; i++) {
                var driver = realDrivers.get(i);
                Map<String, Object> driverData = new HashMap<>();
                driverData.put("name", driver.getFirstName() + " " + driver.getLastName());
                driverData.put("routes", 20 + random.nextInt(50));
                driverData.put("revenue", 300000 + random.nextInt(500000));
                driverData.put("efficiency", 80 + random.nextInt(20));
                driverData.put("rating", 4.2 + random.nextDouble() * 0.8);
                driverData.put("violations", random.nextInt(3));
                drivers.add(driverData);
            }
        } catch (Exception e) {
            log.warn("Не удалось получить данные водителей, генерируем тестовые: {}", e.getMessage());
            // Fallback - генерируем тестовые данные
            for (int i = 0; i < limit; i++) {
                Map<String, Object> driverData = new HashMap<>();
                driverData.put("name", "Водитель " + (i + 1));
                driverData.put("routes", 20 + random.nextInt(50));
                driverData.put("revenue", 300000 + random.nextInt(500000));
                driverData.put("efficiency", 80 + random.nextInt(20));
                driverData.put("rating", 4.2 + random.nextDouble() * 0.8);
                driverData.put("violations", random.nextInt(3));
                drivers.add(driverData);
            }
        }

        return drivers;
    }

    private List<Map<String, Object>> generatePopularRoutes(Integer limit) {
        List<Map<String, Object>> routes = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Популярные направления
        String[][] popularRoutes = {
            {"Москва", "Санкт-Петербург"},
            {"Москва", "Екатеринбург"},
            {"Санкт-Петербург", "Новгород"},
            {"Москва", "Нижний Новгород"},
            {"Казань", "Самара"}
        };

        for (int i = 0; i < Math.min(limit, popularRoutes.length); i++) {
            Map<String, Object> routeData = new HashMap<>();
            String routeName = popularRoutes[i][0] + " - " + popularRoutes[i][1];
            
            routeData.put("route", routeName);
            routeData.put("frequency", 5 + random.nextInt(20));
            routeData.put("revenue", 50000 + random.nextInt(200000));
            routeData.put("profit", 15000 + random.nextInt(60000));
            routeData.put("averageTime", (8 + random.nextInt(16)) + " ч " + random.nextInt(60) + " мин");
            routeData.put("efficiency", 80 + random.nextDouble() * 15);
            routes.add(routeData);
        }

        return routes;
    }
} 
package ru.maslov.trucknavigator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.maslov.trucknavigator.service.analytics.CompanyAnalyticsService;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Аналитика компании", description = "API для общей аналитики компании")
public class CompanyAnalyticsController {

    private final CompanyAnalyticsService companyAnalyticsService;

    @GetMapping("/company")
    @Operation(summary = "Общая аналитика компании",
            description = "Получение KPI и общих показателей компании")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getCompanyAnalytics(
            @RequestParam(required = false, defaultValue = "6m") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Map<String, Object> analytics = companyAnalyticsService.getCompanyAnalytics(period, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/drivers")
    @Operation(summary = "Аналитика по водителям",
            description = "Получение показателей эффективности водителей")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDriversAnalytics(
            @RequestParam(required = false, defaultValue = "6m") String period,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        Map<String, Object> analytics = companyAnalyticsService.getDriversAnalytics(period, limit);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/routes")
    @Operation(summary = "Аналитика по маршрутам",
            description = "Получение статистики по популярным маршрутам")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getRoutesAnalytics(
            @RequestParam(required = false, defaultValue = "6m") String period,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {

        Map<String, Object> analytics = companyAnalyticsService.getRoutesAnalytics(period, limit);
        return ResponseEntity.ok(analytics);
    }
} 
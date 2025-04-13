package ru.maslov.trucknavigator.dto.fuel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для представления информации о цене на топливо.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelPriceDto {

    private String fuelType;          // Тип топлива: DIESEL, PETROL_92, PETROL_95, PETROL_98
    private BigDecimal price;         // Цена за литр
    private String currency;          // Валюта (RUB, USD, EUR)
    private double latitude;          // Широта АЗС
    private double longitude;         // Долгота АЗС
    private String stationName;       // Название АЗС
    private String company;           // Компания-владелец АЗС
    private long updateTime;          // Время последнего обновления данных (unix timestamp)
    private Integer distance;         // Расстояние от маршрута в метрах (опционально)
}
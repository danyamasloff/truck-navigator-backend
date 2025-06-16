package ru.maslov.trucknavigator.dto.fuel;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для представления ответа API с ценами на топливо.
 */
@Data
public class FuelPriceResponse {

    private boolean success;                        // Успешность запроса
    private String message;                         // Сообщение об ошибке (если есть)
    private List<FuelPriceDto> data = new ArrayList<>();  // Список цен на топливо
    private BigDecimal minPrice;                    // Минимальная цена
    private BigDecimal maxPrice;                    // Максимальная цена
    private BigDecimal averagePrice;                // Средняя цена
}

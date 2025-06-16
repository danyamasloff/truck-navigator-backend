package ru.maslov.trucknavigator.dto.geocoding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для отображения места на карте.
 * Содержит информацию для отображения результатов поиска на фронтенде.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationDto {
    private Long id;            // Генерируется на бэкенде для уникальной идентификации в списке
    private String name;        // Название места или адрес
    private String description; // Дополнительное описание (город, регион и т.д.)
    private double latitude;    // Широта
    private double longitude;   // Долгота
    private String type;        // Тип места (АЗС, склад, ресторан и т.д.)
    private String provider;    // Источник данных (GraphHopper, OSM и т.д.)
}

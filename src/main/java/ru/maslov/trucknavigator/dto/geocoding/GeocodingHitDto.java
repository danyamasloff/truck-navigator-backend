package ru.maslov.trucknavigator.dto.geocoding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO для отдельного результата геокодинга.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GeocodingHitDto {
    private GeoPoint point;
    private String name;
    private String country;
    private String city;
    private String state;
    private String street;
    private String housenumber;
    private String postcode;
    private String osmId;
    private String osmType;
    private String osmKey;
    private String osmValue;
    private Map<String, String> tags;
}

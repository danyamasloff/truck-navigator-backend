package ru.maslov.trucknavigator.dto.geocoding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO, представляющий ответ API геокодинга.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeocodingResultDto {
    private List<GeocodingHitDto> hits;
    private String locale;
}


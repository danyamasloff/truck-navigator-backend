package ru.maslov.trucknavigator.dto.geocoding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {
    private double lat;
    private double lng;
}
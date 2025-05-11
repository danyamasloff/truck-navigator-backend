package ru.maslov.trucknavigator.dto.weather;

public enum WeatherHazardType {
    STRONG_WIND,   // Сильный ветер (опасность опрокидывания)
    ICE_RISK,      // Риск обледенения дороги
    LOW_VISIBILITY, // Плохая видимость (туман)
    HEAVY_RAIN,    // Сильный дождь
    SNOW,          // Снегопад
    THUNDERSTORM,  // Гроза
    EXTREME_COLD,  // Экстремально низкая температура
    EXTREME_HEAT   // Экстремально высокая температура
}
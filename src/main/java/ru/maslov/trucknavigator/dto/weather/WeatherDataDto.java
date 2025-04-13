
package ru.maslov.trucknavigator.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO с данными о погоде для конкретной точки и времени.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDataDto {

    // Базовая информация
    private String cityName;
    private LocalDateTime forecastTime;

    // Основные показатели
    private double temperature;       // Температура в градусах Цельсия
    private double feelsLike;         // Ощущаемая температура
    private int humidity;             // Влажность в процентах
    private int pressure;             // Атмосферное давление в гектопаскалях

    // Ветер
    private double windSpeed;         // Скорость ветра в м/с
    private int windDirection;        // Направление ветра в градусах
    private Double windGust;          // Порывы ветра в м/с

    // Осадки
    private Double rainVolume1h;      // Количество осадков за 1 час в мм
    private Double rainVolume3h;      // Количество осадков за 3 часа в мм
    private Double snowVolume1h;      // Количество снега за 1 час в мм
    private Double snowVolume3h;      // Количество снега за 3 часа в мм

    // Облачность и видимость
    private int cloudiness;           // Облачность в процентах
    private Integer visibility;       // Видимость в метрах

    // Восход и закат
    private LocalDateTime sunrise;
    private LocalDateTime sunset;

    // Описание погоды
    private int weatherId;            // Идентификатор погодного условия
    private String weatherMain;       // Основная категория погоды (Rain, Snow, ...)
    private String weatherDescription; // Описание погоды
    private String weatherIcon;       // Идентификатор иконки погоды

    // Расчетные показатели для оценки риска
    private Integer riskScore;        // Оценка риска от 0 до 100
    private String riskLevel;         // Уровень риска: LOW, MODERATE, HIGH, SEVERE
    private String riskDescription;   // Описание риска

    /**
     * Определяет уровень риска на основе погодных условий.
     *
     * @return строковое представление уровня риска
     */
    public String calculateRiskLevel() {
        int score = 0;

        // Оценка по температуре
        if (temperature < -20) score += 30;
        else if (temperature < -10) score += 20;
        else if (temperature < 0) score += 10;
        else if (temperature > 35) score += 15;

        // Оценка по осадкам
        if (rainVolume1h != null) {
            if (rainVolume1h > 10) score += 30;
            else if (rainVolume1h > 5) score += 20;
            else if (rainVolume1h > 2) score += 10;
        }

        if (snowVolume1h != null) {
            if (snowVolume1h > 5) score += 40;
            else if (snowVolume1h > 2) score += 25;
            else if (snowVolume1h > 0.5) score += 15;
        }

        // Оценка по ветру
        if (windSpeed > 20) score += 35;
        else if (windSpeed > 15) score += 25;
        else if (windSpeed > 10) score += 15;
        else if (windSpeed > 5) score += 5;

        // Оценка по видимости
        if (visibility != null) {
            if (visibility < 100) score += 50;
            else if (visibility < 500) score += 35;
            else if (visibility < 1000) score += 20;
            else if (visibility < 2000) score += 10;
        }

        // Ограничиваем максимальное значение
        score = Math.min(score, 100);

        // Устанавливаем числовую оценку
        this.riskScore = score;

        // Определяем уровень риска
        if (score >= 70) {
            this.riskLevel = "SEVERE";
            return "SEVERE";
        } else if (score >= 50) {
            this.riskLevel = "HIGH";
            return "HIGH";
        } else if (score >= 30) {
            this.riskLevel = "MODERATE";
            return "MODERATE";
        } else {
            this.riskLevel = "LOW";
            return "LOW";
        }
    }

    /**
     * Формирует описание риска на основе погодных условий.
     *
     * @return описание риска
     */
    public String generateRiskDescription() {
        StringBuilder description = new StringBuilder();

        // Проверяем различные погодные условия и формируем соответствующие описания
        if (weatherMain != null) {
            switch (weatherMain.toUpperCase()) {
                case "THUNDERSTORM":
                    description.append("Риск грозы. ");
                    break;
                case "DRIZZLE":
                case "RAIN":
                    if (rainVolume1h != null && rainVolume1h > 5) {
                        description.append("Сильный дождь, снижение видимости и сцепления с дорогой. ");
                    } else {
                        description.append("Дождь, возможно снижение сцепления с дорогой. ");
                    }
                    break;
                case "SNOW":
                    description.append("Снегопад, сложные дорожные условия, снижение видимости. ");
                    break;
                case "FOG":
                case "MIST":
                    description.append("Туман, сильно ограниченная видимость. ");
                    break;
            }
        }

        // Добавляем информацию о ветре
        if (windSpeed > 15) {
            description.append("Сильный ветер, опасность для высоких ТС. ");
        } else if (windSpeed > 10) {
            description.append("Умеренный ветер, возможно влияние на управляемость. ");
        }

        // Добавляем информацию о температуре
        if (temperature < -15) {
            description.append("Экстремально низкая температура, риск замерзания топлива и систем. ");
        } else if (temperature < -5) {
            description.append("Низкая температура, возможно обледенение дороги. ");
        } else if (temperature > 30) {
            description.append("Высокая температура, риск перегрева двигателя. ");
        }

        // Если нет особых условий
        if (description.length() == 0) {
            description.append("Благоприятные погодные условия, без особых рисков. ");
        }

        this.riskDescription = description.toString().trim();
        return this.riskDescription;
    }
}
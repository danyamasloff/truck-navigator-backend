package ru.maslov.trucknavigator.dto.routing;

// Импорты Lombok (если еще не добавлены глобально)
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Импорт DTO для погоды
import ru.maslov.trucknavigator.dto.weather.WeatherDataDto; // <--- Важно: Убедитесь, что путь к WeatherDataDto правильный

// Импорты стандартных Java классов
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Импорт для игнорирования неизвестных полей при парсинге JSON
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO, содержащий расширенную информацию о маршруте, включая данные о погоде в ключевых точках.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDetailsResponseDto {

    // Основная информация о маршруте
    private BigDecimal distance; // в километрах (Нужно будет конвертировать из метров GraphHopper)
    private long duration;       // в минутах (Нужно будет конвертировать из миллисекунд GraphHopper)
    private String startAddress; // Получать через реверс-геокодинг или из запроса
    private String endAddress;   // Получать через реверс-геокодинг или из запроса

    // Точки маршрута с информацией о погоде
    private List<RoutePointDto> points = new ArrayList<>();

    // Общая информация о погодных условиях на маршруте
    private WeatherSummaryDto weatherSummary;

    // Дополнительная информация для фронтенда
    // Используем List<List<Double>> для [longitude, latitude] пар от GraphHopper (если points_encoded=false)
    private List<List<Double>> coordinates; // Координаты для отображения на карте
    private List<Instruction> instructions; // Инструкции для навигации (Используем вложенный DTO)

    /**
     * DTO для представления точки маршрута с погодными данными.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutePointDto {
        private double latitude;
        private double longitude;
        private String pointType; // START, END, WAYPOINT, MIDPOINT
        private String description; // Описание точки
        private double distanceFromStart; // Расстояние от начала маршрута в км
        // --- ИСПРАВЛЕНО ТУТ ---
        private WeatherDataDto weather; // Данные о погоде в этой точке (Используем существующий DTO)
        // --- КОНЕЦ ИСПРАВЛЕНИЯ ---
    }

    /**
     * DTO для представления обобщенной информации о погоде на маршруте.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherSummaryDto {
        private boolean hasHazardousConditions; // Есть ли опасные погодные условия
        private String hazardDescription; // Описание опасных условий
        private double temperatureMin; // Минимальная температура на маршруте
        private double temperatureMax; // Максимальная температура на маршруте
        private List<String> weatherConditions = new ArrayList<>(); // Список погодных условий на маршруте
    }

    /**
     * DTO для представления инструкции по маршруту от GraphHopper.
     * Адаптируйте поля под реальный ответ API GraphHopper.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true) // Игнорировать поля, которых нет в DTO
    public static class Instruction {
        private String text;       // Текст инструкции
        private double distance;   // Дистанция этого шага (в метрах)
        private long time;         // Время выполнения этого шага (в мс)
        // private List<Integer> interval; // Индексы точек геометрии для этого шага (если нужно)
        // private Integer sign;           // Код знака маневра (если нужно)
    }
}
package ru.maslov.trucknavigator.exception;

/**
 * Исключение, возникающее при ошибках в API погоды.
 */
public class WeatherApiException extends TruckNavigatorException {

    public WeatherApiException(String message) {
        super(message);
    }

    public WeatherApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

package ru.maslov.trucknavigator.exception;

/**
 * Базовое исключение для всех исключений в приложении.
 */
public class TruckNavigatorException extends RuntimeException {

    public TruckNavigatorException(String message) {
        super(message);
    }

    public TruckNavigatorException(String message, Throwable cause) {
        super(message, cause);
    }
}

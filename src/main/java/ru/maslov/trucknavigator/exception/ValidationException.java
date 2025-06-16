package ru.maslov.trucknavigator.exception;

/**
 * Исключение, возникающее при ошибках валидации данных.
 */
public class ValidationException extends TruckNavigatorException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

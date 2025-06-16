package ru.maslov.trucknavigator.exception;

/**
 * Исключение, возникающее при ошибках маршрутизации.
 */
public class RoutingException extends TruckNavigatorException {

    public RoutingException(String message) {
        super(message);
    }

    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}

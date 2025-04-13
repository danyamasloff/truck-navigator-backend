package ru.maslov.trucknavigator.exception;

/**
 * Исключение, возникающее при ошибках интеграции с внешними сервисами.
 */
public class ExternalServiceException extends TruckNavigatorException {

    public ExternalServiceException(String serviceName, String message) {
        super(String.format("Ошибка в сервисе %s: %s", serviceName, message));
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(String.format("Ошибка в сервисе %s: %s", serviceName, message), cause);
    }
}
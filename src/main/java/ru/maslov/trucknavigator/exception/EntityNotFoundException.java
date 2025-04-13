package ru.maslov.trucknavigator.exception;

/**
 * Исключение, возникающее при ошибках в работе с сущностями.
 */
public class EntityNotFoundException extends TruckNavigatorException {

    public EntityNotFoundException(String entityType, Long id) {
        super(String.format("%s с идентификатором %d не найден", entityType, id));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
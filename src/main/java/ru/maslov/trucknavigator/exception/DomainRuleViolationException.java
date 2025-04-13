package ru.maslov.trucknavigator.exception;

/**
 * Исключение, возникающее при нарушении правил предметной области.
 */
public class DomainRuleViolationException extends TruckNavigatorException {

    public DomainRuleViolationException(String message) {
        super(message);
    }

    public DomainRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
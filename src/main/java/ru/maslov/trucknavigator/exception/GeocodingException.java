package ru.maslov.trucknavigator.exception;

public class GeocodingException extends TruckNavigatorException {

    public GeocodingException(String message) {
        super(message);
    }

    public GeocodingException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.wqms.watermonitor.exception;

/**
 * Thrown when incoming sensor data fails business-level validation
 * (e.g., values outside physically possible ranges).
 */
public class InvalidSensorDataException extends RuntimeException {

    public InvalidSensorDataException(String message) {
        super(message);
    }
}

package com.wqms.watermonitor.exception;

/**
 * Thrown when a requested resource (e.g., a sensor reading by id)
 * cannot be found in the database.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

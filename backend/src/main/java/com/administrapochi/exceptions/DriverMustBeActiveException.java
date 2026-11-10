package com.administrapochi.exceptions;

public class DriverMustBeActiveException extends RuntimeException {

    public DriverMustBeActiveException(Integer driverId) {
        super("Absences can only be registered for active driver with id: " + driverId);
    }
}

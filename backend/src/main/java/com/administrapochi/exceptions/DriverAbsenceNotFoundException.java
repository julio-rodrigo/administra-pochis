package com.administrapochi.exceptions;

public class DriverAbsenceNotFoundException extends RuntimeException {

    public DriverAbsenceNotFoundException(Integer driverId, Integer absenceId) {
        super("Absence with id " + absenceId + " not found for driver with id: " + driverId);
    }
}

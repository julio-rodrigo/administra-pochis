package com.administrapochi.exceptions;

public class DriverLicenseNotFoundException extends RuntimeException {

    public DriverLicenseNotFoundException(Integer driverId) {
        super("Current license not found for driver with id: " + driverId);
    }

    public DriverLicenseNotFoundException(Integer driverId, Integer licenseId) {
        super("License with id " + licenseId + " not found for driver with id: " + driverId);
    }
}

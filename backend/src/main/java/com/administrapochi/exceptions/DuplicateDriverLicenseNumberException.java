package com.administrapochi.exceptions;

public class DuplicateDriverLicenseNumberException extends RuntimeException {

    public DuplicateDriverLicenseNumberException(String licenseNumber) {
        super("A driver license with number '" + licenseNumber + "' already exists");
    }
}

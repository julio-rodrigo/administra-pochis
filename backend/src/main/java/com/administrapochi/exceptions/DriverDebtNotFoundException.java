package com.administrapochi.exceptions;

public class DriverDebtNotFoundException extends RuntimeException {

    public DriverDebtNotFoundException(Integer id) {
        super("Driver Debt with id " + id + " not found.");
    }
}

package com.administrapochi.exceptions;

public class DriverNotFoundException extends RuntimeException {

    public DriverNotFoundException(Integer id) {
        super("Driver not found with id: " + id);
    }
}

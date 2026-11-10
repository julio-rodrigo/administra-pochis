package com.administrapochi.exceptions;

public class DriverDeletedException extends RuntimeException {

    public DriverDeletedException(Integer id) {
        super("Driver with id " + id + " has been deleted");
    }
}

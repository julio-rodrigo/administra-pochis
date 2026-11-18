package com.administrapochi.exceptions;

public class PochiDailyStatusNotFoundException extends RuntimeException {
    public PochiDailyStatusNotFoundException(Integer id) {
        super("Pochi daily status with id " + id + " was not found.");
    }
}

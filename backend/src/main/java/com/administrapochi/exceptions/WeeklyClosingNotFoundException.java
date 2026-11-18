package com.administrapochi.exceptions;

public class WeeklyClosingNotFoundException extends RuntimeException {

    public WeeklyClosingNotFoundException(Integer id) {
        super("Weekly closing with id " + id + " was not found");
    }
}

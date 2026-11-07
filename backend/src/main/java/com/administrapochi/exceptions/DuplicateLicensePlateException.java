package com.administrapochi.exceptions;

public class DuplicateLicensePlateException extends RuntimeException {

    public DuplicateLicensePlateException(String licensePlate) {
        super("A pochi with license plate '" + licensePlate + "' already exists");
    }
}

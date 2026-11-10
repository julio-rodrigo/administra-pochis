package com.administrapochi.exceptions;

public class InvalidDriverLicenseDateRangeException extends RuntimeException {

    public InvalidDriverLicenseDateRangeException() {
        super("License expiration date must be after its issue date");
    }
}

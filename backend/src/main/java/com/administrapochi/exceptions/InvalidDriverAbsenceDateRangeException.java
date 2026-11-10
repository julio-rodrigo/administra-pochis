package com.administrapochi.exceptions;

public class InvalidDriverAbsenceDateRangeException extends RuntimeException {

    public InvalidDriverAbsenceDateRangeException() {
        super("Absence end date must be on or after its start date");
    }
}

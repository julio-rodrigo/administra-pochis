package com.administrapochi.exceptions;

public class DuplicateEconomicNumberException extends RuntimeException {

    public DuplicateEconomicNumberException(String economicNumber) {
        super("A pochi with economic number '" + economicNumber + "' already exists");
    }
}

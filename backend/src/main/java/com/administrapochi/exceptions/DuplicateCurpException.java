package com.administrapochi.exceptions;

public class DuplicateCurpException extends RuntimeException {

    public DuplicateCurpException(String curp) {
        super("A driver with CURP '" + curp + "' already exists");
    }
}

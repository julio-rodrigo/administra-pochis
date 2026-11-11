package com.administrapochi.exceptions;

public class PochiAssignmentPochiInactiveException extends RuntimeException {

    public PochiAssignmentPochiInactiveException(Integer id) {
        super("Pochi with id " + id + " is inactive and cannot be assigned.");
    }
}

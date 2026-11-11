package com.administrapochi.exceptions;

public class PochiAssignmentDriverInactiveException extends RuntimeException {

    public PochiAssignmentDriverInactiveException(Integer id) {
        super("Driver with id " + id + " is inactive and cannot be assigned.");
    }
}

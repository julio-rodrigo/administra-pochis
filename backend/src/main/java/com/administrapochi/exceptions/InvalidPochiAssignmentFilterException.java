package com.administrapochi.exceptions;

public class InvalidPochiAssignmentFilterException extends RuntimeException {

    public InvalidPochiAssignmentFilterException() {
        super("The assignment date range is invalid.");
    }
}

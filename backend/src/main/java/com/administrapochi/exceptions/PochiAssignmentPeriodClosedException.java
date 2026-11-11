package com.administrapochi.exceptions;

public class PochiAssignmentPeriodClosedException extends RuntimeException {

    public PochiAssignmentPeriodClosedException() {
        super("The assignment cannot be changed because its administrative period is closed.");
    }
}

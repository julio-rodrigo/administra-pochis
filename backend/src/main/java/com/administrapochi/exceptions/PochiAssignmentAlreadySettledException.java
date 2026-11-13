package com.administrapochi.exceptions;

public class PochiAssignmentAlreadySettledException extends RuntimeException {

    public PochiAssignmentAlreadySettledException(Integer id) {
        super("Pochi assignment is already settled: " + id);
    }
}

package com.administrapochi.exceptions;

public class PochiAssignmentNotFoundException extends RuntimeException {

    public PochiAssignmentNotFoundException(Integer id) {
        super("Pochi assignment not found with id: " + id);
    }
}

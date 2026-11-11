package com.administrapochi.exceptions;

public class PochiAssignmentReferencedException extends RuntimeException {

    public PochiAssignmentReferencedException(Integer id) {
        super("The Pochi assignment with id " + id
                + " cannot be modified or deleted because it is referenced by another record.");
    }
}

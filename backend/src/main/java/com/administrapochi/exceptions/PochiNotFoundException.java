package com.administrapochi.exceptions;

public class PochiNotFoundException extends RuntimeException {

    public PochiNotFoundException(Integer id) {
        super("Pochi not found with id: " + id);
    }
}

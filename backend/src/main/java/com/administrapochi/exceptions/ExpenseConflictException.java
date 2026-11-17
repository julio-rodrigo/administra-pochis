package com.administrapochi.exceptions;

public class ExpenseConflictException extends RuntimeException {

    public ExpenseConflictException(String message) {
        super(message);
    }
}

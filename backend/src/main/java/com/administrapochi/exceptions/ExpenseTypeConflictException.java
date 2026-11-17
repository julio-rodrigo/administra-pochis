package com.administrapochi.exceptions;

public class ExpenseTypeConflictException extends RuntimeException {

    public ExpenseTypeConflictException(String message) {
        super(message);
    }
}

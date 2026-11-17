package com.administrapochi.exceptions;

public class ExpenseTypeNotFoundException extends RuntimeException {

    public ExpenseTypeNotFoundException(Integer id) {
        super("Expense Type with id " + id + " was not found");
    }
}

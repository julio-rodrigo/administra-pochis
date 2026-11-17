package com.administrapochi.exceptions;

public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(Integer id) {
        super("Expense with id " + id + " was not found");
    }
}

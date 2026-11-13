package com.administrapochi.exceptions;

public class InvalidDriverDebtFilterException extends RuntimeException {

    public InvalidDriverDebtFilterException() {
        super("debtDateTo must be on or after debtDateFrom.");
    }
}

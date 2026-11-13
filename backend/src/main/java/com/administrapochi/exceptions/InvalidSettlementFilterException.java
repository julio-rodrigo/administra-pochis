package com.administrapochi.exceptions;

public class InvalidSettlementFilterException extends RuntimeException {

    public InvalidSettlementFilterException() {
        super("workDateFrom must be on or before workDateTo, and cannot be combined with workDate.");
    }
}

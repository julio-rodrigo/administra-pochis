package com.administrapochi.exceptions;

public class InvalidSettlementRuleDateRangeException extends RuntimeException {

    public InvalidSettlementRuleDateRangeException() {
        super("validTo must be on or after validFrom.");
    }
}

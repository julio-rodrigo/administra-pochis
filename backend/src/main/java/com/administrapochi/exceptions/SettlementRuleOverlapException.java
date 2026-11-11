package com.administrapochi.exceptions;

public class SettlementRuleOverlapException extends RuntimeException {

    public SettlementRuleOverlapException() {
        super("A Settlement Rule already exists for the same scope during the requested validity period.");
    }
}

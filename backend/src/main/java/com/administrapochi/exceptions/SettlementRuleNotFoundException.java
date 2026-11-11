package com.administrapochi.exceptions;

public class SettlementRuleNotFoundException extends RuntimeException {

    public SettlementRuleNotFoundException(Integer id) {
        super("Settlement Rule not found with id: " + id);
    }
}

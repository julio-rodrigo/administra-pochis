package com.administrapochi.exceptions;

public class SettlementNotFoundException extends RuntimeException {

    public SettlementNotFoundException(Integer id) {
        super("Settlement not found with id: " + id);
    }
}

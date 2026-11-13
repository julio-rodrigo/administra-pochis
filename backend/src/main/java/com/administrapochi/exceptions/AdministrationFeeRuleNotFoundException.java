package com.administrapochi.exceptions;

public class AdministrationFeeRuleNotFoundException extends RuntimeException {

    public AdministrationFeeRuleNotFoundException(Integer id) {
        super("Administration fee rule with ID " + id + " was not found.");
    }
}

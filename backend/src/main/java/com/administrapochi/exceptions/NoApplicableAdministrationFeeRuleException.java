package com.administrapochi.exceptions;

import java.time.LocalDate;

public class NoApplicableAdministrationFeeRuleException extends RuntimeException {

    public NoApplicableAdministrationFeeRuleException(Integer pochiId, LocalDate date) {
        super("No administration fee rule applies to Pochi " + pochiId + " on " + date + ".");
    }
}

package com.administrapochi.exceptions;

import java.time.LocalDate;

public class NoApplicableSettlementRuleException extends RuntimeException {

    public NoApplicableSettlementRuleException(Integer pochiId, Integer shiftCount, LocalDate date) {
        super("No applicable Settlement Rule found for "
                + (pochiId == null ? "the general scope" : "Pochi with id: " + pochiId)
                + ", shift count: " + shiftCount + ", date: " + date + ".");
    }
}

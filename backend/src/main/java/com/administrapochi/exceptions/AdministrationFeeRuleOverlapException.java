package com.administrapochi.exceptions;

public class AdministrationFeeRuleOverlapException extends RuntimeException {

    public AdministrationFeeRuleOverlapException(Integer pochiId) {
        super(pochiId == null
                ? "An administration fee global rule already exists for the requested validity period."
                : "An administration fee rule already exists for Pochi " + pochiId
                        + " during the requested validity period.");
    }
}

package com.administrapochi.models.dtos.settlementrules;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SettlementRuleRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidShiftCountsAndAmounts() {
        SettlementRuleCreateRequest request = new SettlementRuleCreateRequest();
        request.setShiftCount(1);
        request.setAmount(new BigDecimal("250.00"));
        request.setValidFrom(LocalDate.of(2026, 1, 1));

        assertEquals(0, validator.validate(request).size());

        request.setShiftCount(2);
        assertEquals(0, validator.validate(request).size());
    }

    @Test
    void rejectsInvalidShiftCountAndNonPositiveAmount() {
        SettlementRuleCreateRequest request = new SettlementRuleCreateRequest();
        request.setShiftCount(3);
        request.setAmount(BigDecimal.ZERO);
        request.setValidFrom(LocalDate.of(2026, 1, 1));

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(2, violations.size());
    }

    @Test
    void rejectsNegativeAmountsAndAmountsWithMoreThanTwoDecimalPlaces() {
        SettlementRuleCreateRequest request = new SettlementRuleCreateRequest();
        request.setShiftCount(1);
        request.setValidFrom(LocalDate.of(2026, 1, 1));
        request.setAmount(new BigDecimal("-1.00"));
        assertFalse(validator.validate(request).isEmpty());

        request.setAmount(new BigDecimal("1.001"));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsValidToBeforeValidFrom() {
        SettlementRuleCreateRequest request = new SettlementRuleCreateRequest();
        request.setShiftCount(2);
        request.setAmount(new BigDecimal("400.00"));
        request.setValidFrom(LocalDate.of(2026, 11, 2));
        request.setValidTo(LocalDate.of(2026, 11, 1));

        assertFalse(validator.validate(request).isEmpty());
    }
}

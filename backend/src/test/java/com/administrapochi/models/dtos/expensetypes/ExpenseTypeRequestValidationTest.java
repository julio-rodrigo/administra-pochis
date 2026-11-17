package com.administrapochi.models.dtos.expensetypes;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExpenseTypeRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidCreateAndPartialUpdateRequests() {
        ExpenseTypeCreateRequest createRequest = new ExpenseTypeCreateRequest();
        createRequest.setCode("INSURANCE");
        createRequest.setName("Insurance");
        createRequest.setDescription("Coverage");

        ExpenseTypeUpdateRequest updateRequest = new ExpenseTypeUpdateRequest();
        updateRequest.setActive(false);

        assertEquals(0, validator.validate(createRequest).size());
        assertEquals(0, validator.validate(updateRequest).size());
    }

    @Test
    void rejectsMissingOrBlankRequiredCreateFields() {
        ExpenseTypeCreateRequest request = new ExpenseTypeCreateRequest();

        var violations = validator.validate(request);

        assertEquals(2, violations.size());
    }

    @Test
    void rejectsFieldsThatExceedDatabaseLengths() {
        ExpenseTypeCreateRequest request = new ExpenseTypeCreateRequest();
        request.setCode("C".repeat(51));
        request.setName("N".repeat(101));
        request.setDescription("D".repeat(256));

        assertEquals(3, validator.validate(request).size());

        ExpenseTypeUpdateRequest updateRequest = new ExpenseTypeUpdateRequest();
        updateRequest.setCode("C".repeat(51));
        updateRequest.setName("N".repeat(101));
        updateRequest.setDescription("D".repeat(256));
        assertEquals(3, validator.validate(updateRequest).size());
    }

    @Test
    void rejectsBlankStringsWhenProvidedForUpdate() {
        ExpenseTypeUpdateRequest request = new ExpenseTypeUpdateRequest();
        request.setCode("   ");
        request.setName("  ");

        assertFalse(validator.validate(request).isEmpty());
        assertEquals(2, validator.validate(request).size());
    }
}

package com.administrapochi.models.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseTypeTest {

    @Test
    void applyCreationDefaultsSetsFlagsAndTimestamps() {
        ExpenseType expenseType = new ExpenseType();

        expenseType.applyCreationDefaults();

        assertTrue(expenseType.getRequiresPochi());
        assertTrue(expenseType.getActive());
        assertNotNull(expenseType.getCreatedAt());
        assertEquals(expenseType.getCreatedAt(), expenseType.getUpdatedAt());
    }

    @Test
    void applyCreationDefaultsPreservesProvidedSystemValues() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 0, 0);
        ExpenseType expenseType = ExpenseType.builder()
                .requiresPochi(false)
                .active(false)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        expenseType.applyCreationDefaults();

        assertEquals(false, expenseType.getRequiresPochi());
        assertEquals(false, expenseType.getActive());
        assertEquals(createdAt, expenseType.getCreatedAt());
        assertEquals(updatedAt, expenseType.getUpdatedAt());
    }

    @Test
    void updateTimestampRefreshesUpdatedAtWithoutChangingCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        ExpenseType expenseType = ExpenseType.builder()
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();

        expenseType.updateTimestamp();

        assertEquals(createdAt, expenseType.getCreatedAt());
        assertTrue(expenseType.getUpdatedAt().isAfter(createdAt));
    }
}

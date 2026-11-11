package com.administrapochi.models.entities;

import com.administrapochi.models.enums.AssignmentShiftOption;
import com.administrapochi.models.enums.Shift;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PochiAssignmentTest {

    @Test
    void lifecycleCallbacksSetAndUpdateAuditTimestamps() {
        PochiAssignment assignment = new PochiAssignment();
        LocalDateTime beforeCreate = LocalDateTime.now();

        assignment.applyCreationTimestamps();
        LocalDateTime createdAt = assignment.getCreatedAt();
        LocalDateTime beforeUpdate = LocalDateTime.now();
        assignment.updateTimestamp();

        assertAll(
                () -> assertNotNull(createdAt),
                () -> assertTrue(!createdAt.isBefore(beforeCreate)),
                () -> assertEquals(createdAt, assignment.getCreatedAt()),
                () -> assertTrue(!assignment.getUpdatedAt().isBefore(beforeUpdate)));
    }

    @Test
    void persistedShiftExcludesFullDayAndDatabaseConstraintsProtectBothSchedulingRules() {
        assertEquals(Arrays.asList(Shift.MORNING, Shift.AFTERNOON), Arrays.asList(Shift.values()));
        assertTrue(Arrays.asList(AssignmentShiftOption.values()).contains(AssignmentShiftOption.FULL_DAY));

        UniqueConstraint[] constraints = PochiAssignment.class.getAnnotation(Table.class).uniqueConstraints();
        assertEquals(2, constraints.length);
        assertTrue(Arrays.stream(constraints).anyMatch(constraint ->
                Arrays.equals(
                        new String[]{"assignment_date", "pochi_id", "shift"},
                        constraint.columnNames())));
        assertTrue(Arrays.stream(constraints).anyMatch(constraint ->
                Arrays.equals(
                        new String[]{"assignment_date", "driver_id", "shift"},
                        constraint.columnNames())));
    }
}

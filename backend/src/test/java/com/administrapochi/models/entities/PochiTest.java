package com.administrapochi.models.entities;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PochiTest {

    @Test
    void applyCreationDefaultsSetsDomainDefaultsAndTimestamps() {
        Pochi pochi = new Pochi();
        Instant before = Instant.now();

        pochi.applyCreationDefaults();

        Instant after = Instant.now();
        assertAll(
                () -> assertEquals("UNTRAC", pochi.getUnionName()),
                () -> assertEquals(Boolean.TRUE, pochi.getActive()),
                () -> assertNotNull(pochi.getCreatedAt()),
                () -> assertNotNull(pochi.getUpdatedAt()),
                () -> assertTrue(!pochi.getCreatedAt().isBefore(before)
                        && !pochi.getCreatedAt().isAfter(after)),
                () -> assertEquals(pochi.getCreatedAt(), pochi.getUpdatedAt()));
    }

    @Test
    void applyCreationDefaultsPreservesProvidedValues() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        Pochi pochi = Pochi.builder()
                .unionName("CUSTOM")
                .active(false)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();

        pochi.applyCreationDefaults();

        assertAll(
                () -> assertEquals("CUSTOM", pochi.getUnionName()),
                () -> assertEquals(Boolean.FALSE, pochi.getActive()),
                () -> assertEquals(timestamp, pochi.getCreatedAt()),
                () -> assertEquals(timestamp, pochi.getUpdatedAt()));
    }

    @Test
    void updateTimestampChangesUpdatedAtWithoutChangingCreatedAt() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant previousUpdatedAt = Instant.parse("2026-01-02T00:00:00Z");
        Pochi pochi = Pochi.builder()
                .createdAt(createdAt)
                .updatedAt(previousUpdatedAt)
                .build();
        Instant before = Instant.now();

        pochi.updateTimestamp();

        Instant after = Instant.now();
        assertAll(
                () -> assertEquals(createdAt, pochi.getCreatedAt()),
                () -> assertNotNull(pochi.getUpdatedAt()),
                () -> assertTrue(!pochi.getUpdatedAt().isBefore(before)
                        && !pochi.getUpdatedAt().isAfter(after)));
    }
}

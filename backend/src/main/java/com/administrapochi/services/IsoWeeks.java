package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidWeeklyClosingException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;

final class IsoWeeks {

    private IsoWeeks() {}

    static void validateYear(Integer year) {
        if (year == null || year < 1 || year > 9999) {
            throw new InvalidWeeklyClosingException("A valid ISO week year is required.");
        }
    }

    static LocalDate start(Integer year, Integer week) {
        validateYear(year);
        if (week == null || week < 1 || week > 53) {
            throw new InvalidWeeklyClosingException("A valid ISO week number is required.");
        }
        WeekFields iso = WeekFields.ISO;
        LocalDate monday = LocalDate.of(year, 1, 4)
                .with(iso.dayOfWeek(), DayOfWeek.MONDAY.getValue()).plusWeeks(week - 1L);
        if (monday.get(iso.weekBasedYear()) != year) {
            throw new InvalidWeeklyClosingException("ISO week " + week + " does not exist in week year " + year);
        }
        return monday;
    }
}

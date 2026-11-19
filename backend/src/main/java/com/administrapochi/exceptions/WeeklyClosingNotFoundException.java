package com.administrapochi.exceptions;

import java.time.LocalDate;

public class WeeklyClosingNotFoundException extends RuntimeException {

    public WeeklyClosingNotFoundException(Integer id) {
        super("Weekly closing with id " + id + " was not found");
    }

    public WeeklyClosingNotFoundException(LocalDate startDate, LocalDate endDate) {
        super("Weekly Closing for " + startDate + " through " + endDate + " was not found.");
    }
}

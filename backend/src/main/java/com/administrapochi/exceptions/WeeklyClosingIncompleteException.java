package com.administrapochi.exceptions;

import com.administrapochi.models.dtos.weeklyclosings.UnresolvedPochiDayResponse;

import java.util.List;

public class WeeklyClosingIncompleteException extends RuntimeException {

    private final List<UnresolvedPochiDayResponse> unresolvedDays;

    public WeeklyClosingIncompleteException(List<UnresolvedPochiDayResponse> unresolvedDays) {
        super("Weekly closing cannot be completed");
        this.unresolvedDays = List.copyOf(unresolvedDays);
    }

    public List<UnresolvedPochiDayResponse> getUnresolvedDays() {
        return unresolvedDays;
    }
}

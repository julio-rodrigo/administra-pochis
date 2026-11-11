package com.administrapochi.services;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Temporary open-period policy; replace it when Administrative Closures is implemented.
 */
@Component
public class OpenAssignmentPeriodLockPolicy implements AssignmentPeriodLockPolicy {

    @Override
    public void validatePeriodIsOpen(LocalDate date) {
    }
}

package com.administrapochi.services;

import java.time.LocalDate;

public interface AssignmentPeriodLockPolicy {

    void validatePeriodIsOpen(LocalDate date);
}

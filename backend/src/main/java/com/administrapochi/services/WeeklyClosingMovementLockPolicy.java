package com.administrapochi.services;

import com.administrapochi.exceptions.WeeklyClosingConflictException;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import com.administrapochi.repositories.WeeklyClosingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WeeklyClosingMovementLockPolicy {

    private static final List<WeeklyClosingStatus> LOCKING_STATUSES =
            List.of(WeeklyClosingStatus.ADMIN_CLOSED, WeeklyClosingStatus.OWNER_CLOSED);

    private final WeeklyClosingRepository weeklyClosingRepository;

    public void validateUnlocked(Integer pochiId, LocalDate date) {
        if (weeklyClosingRepository.existsProtectedMovementForPochiAndDate(pochiId, date, LOCKING_STATUSES)) {
            throw new WeeklyClosingConflictException(
                    "Movements for Pochi " + pochiId + " on " + date
                            + " are locked by a closed Weekly Closing.");
        }
    }
}

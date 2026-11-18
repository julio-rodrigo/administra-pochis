package com.administrapochi.services;

import com.administrapochi.exceptions.PochiDailyStatusConflictException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.enums.PochiDailyStatusType;
import com.administrapochi.repositories.PochiDailyStatusRepository;
import com.administrapochi.repositories.PochiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class PochiDailyStatusPolicy {

    private final PochiDailyStatusRepository dailyStatusRepository;
    private final PochiRepository pochiRepository;

    public Pochi lockPochi(Integer pochiId) {
        return pochiRepository.findByIdForUpdate(pochiId)
                .orElseThrow(() -> new PochiNotFoundException(pochiId));
    }

    public void lockPochis(Integer firstId, Integer secondId) {
        Arrays.stream(new Integer[]{firstId, secondId}).distinct().sorted().forEach(this::lockPochi);
    }

    public void validateAssignmentAllowed(Integer pochiId, LocalDate date) {
        dailyStatusRepository.findByPochiIdAndStatusDate(pochiId, date).ifPresent(dailyStatus -> {
            if (dailyStatus.getStatus() != PochiDailyStatusType.DAY_OFF) {
                throw new PochiDailyStatusConflictException(
                        "A Pochi Assignment cannot be created or updated for a date marked as "
                                + dailyStatus.getStatus() + ".");
            }
        });
    }

    public void validateFinancialOperationAllowed(Integer pochiId, LocalDate date, String operation) {
        dailyStatusRepository.findByPochiIdAndStatusDate(pochiId, date).ifPresent(dailyStatus -> {
            throw new PochiDailyStatusConflictException(
                    operation + " cannot be created for a date marked as " + dailyStatus.getStatus() + ".");
        });
    }
}

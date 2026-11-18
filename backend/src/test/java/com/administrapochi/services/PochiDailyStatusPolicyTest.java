package com.administrapochi.services;

import com.administrapochi.exceptions.PochiDailyStatusConflictException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiDailyStatus;
import com.administrapochi.models.enums.PochiDailyStatusType;
import com.administrapochi.repositories.PochiDailyStatusRepository;
import com.administrapochi.repositories.PochiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PochiDailyStatusPolicyTest {
    private static final LocalDate DATE = LocalDate.of(2026, 10, 19);
    @Mock private PochiDailyStatusRepository repository;
    @Mock private PochiRepository pochiRepository;
    @InjectMocks private PochiDailyStatusPolicy policy;

    @Test
    void locksPochisInStableOrderAndRejectsMissingPochi() {
        Pochi first = Pochi.builder().id(1).build();
        Pochi second = Pochi.builder().id(2).build();
        when(pochiRepository.findByIdForUpdate(1)).thenReturn(Optional.of(first));
        when(pochiRepository.findByIdForUpdate(2)).thenReturn(Optional.of(second));
        policy.lockPochis(2, 1);
        var order = inOrder(pochiRepository);
        order.verify(pochiRepository).findByIdForUpdate(1);
        order.verify(pochiRepository).findByIdForUpdate(2);
        assertThat(policy.lockPochi(1)).isSameAs(first);
        assertThrows(PochiNotFoundException.class, () -> policy.lockPochi(99));
    }

    @ParameterizedTest
    @EnumSource(PochiDailyStatusType.class)
    void financialOperationsAreBlockedForEveryDailyStatus(PochiDailyStatusType status) {
        when(repository.findByPochiIdAndStatusDate(1, DATE))
                .thenReturn(Optional.of(PochiDailyStatus.builder().status(status).build()));
        PochiDailyStatusConflictException exception = assertThrows(PochiDailyStatusConflictException.class,
                () -> policy.validateFinancialOperationAllowed(1, DATE, "A Settlement"));
        assertThat(exception.getMessage()).contains(status.name());
    }

    @ParameterizedTest
    @EnumSource(PochiDailyStatusType.class)
    void assignmentsAreAllowedOnlyForDayOff(PochiDailyStatusType status) {
        when(repository.findByPochiIdAndStatusDate(1, DATE))
                .thenReturn(Optional.of(PochiDailyStatus.builder().status(status).build()));
        if (status == PochiDailyStatusType.DAY_OFF) {
            assertDoesNotThrow(() -> policy.validateAssignmentAllowed(1, DATE));
        } else {
            assertThrows(PochiDailyStatusConflictException.class, () -> policy.validateAssignmentAllowed(1, DATE));
        }
    }

    @Test
    void noStatusAllowsAssignmentAndFinancialOperation() {
        assertDoesNotThrow(() -> policy.validateAssignmentAllowed(1, DATE));
        assertDoesNotThrow(() -> policy.validateFinancialOperationAllowed(1, DATE, "A Settlement"));
    }
}

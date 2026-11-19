package com.administrapochi.repositories;

import com.administrapochi.models.entities.Expense;
import com.administrapochi.models.enums.ExpenseStatus;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional(readOnly = true)
class ReportReadRepositoryTest {

    @Autowired private ReportReadRepository reports;

    @Test
    void executesPostgresReadQueriesWithoutChangingBusinessData() {
        var page = reports.findHistory(null, List.of(WeeklyClosingStatus.values()), null, null, null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "weekStart", "id")));
        var start = LocalDate.of(2026, 11, 16);
        var end = start.plusDays(6);
        var closingId = page.hasContent() ? page.getContent().getFirst().getId() : Integer.MIN_VALUE;
        var closingIds = List.of(closingId);
        assertThat(reports.sumAcceptedSettlements(closingIds)).hasSizeLessThanOrEqualTo(1);
        assertThat(reports.sumPayments(closingIds)).hasSizeLessThanOrEqualTo(1);
        assertThat(reports.sumCharges(closingIds)).hasSizeLessThanOrEqualTo(1);
        assertThat(reports.sumExpenses(closingIds)).hasSizeLessThanOrEqualTo(1);
        assertThat(reports.findWeek(null, start, end, PageRequest.of(0, 2))).hasSizeLessThanOrEqualTo(2);
        assertThat(reports.findNewDebts(closingId)).doesNotContainNull();
        assertThat(reports.findPayments(closingId)).doesNotContainNull();
        assertThat(reports.findUnresolvedPayments(start, end)).allMatch(p -> !p.getDate().isBefore(start)
                && !p.getDate().isAfter(end));
        assertThat(reports.findGlobalExpenses(start, end)).allMatch(e -> !e.getDate().isBefore(start)
                && !e.getDate().isAfter(end));
        var snapshot = reports.findSnapshotPochis(closingId);
        var ids = List.of(snapshot.isEmpty() ? Integer.MIN_VALUE : snapshot.getFirst().getId());
        assertThat(reports.findExpenses(ids, start, end)).allSatisfy(expense -> {
            assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.ACTIVE);
            assertThat(expense.getPochi().getId()).isIn(ids);
            assertThat(expense.getExpenseDate()).isBetween(start, end);
        }).extracting(Expense::getId).doesNotHaveDuplicates();
    }
}

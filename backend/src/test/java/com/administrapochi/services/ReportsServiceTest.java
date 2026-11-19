package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidWeeklyClosingException;
import com.administrapochi.exceptions.ReportAccessDeniedException;
import com.administrapochi.exceptions.WeeklyClosingNotFoundException;
import com.administrapochi.models.dtos.reports.WeeklyReportResponse.PochiDailyReportResponse;
import com.administrapochi.models.dtos.reports.WeeklyReportResponse.ReportWarningResponse;
import com.administrapochi.models.entities.*;
import com.administrapochi.models.enums.*;
import com.administrapochi.models.mappers.ReportsMapper;
import com.administrapochi.repositories.ReportReadRepository;
import com.administrapochi.repositories.ReportReadRepository.AmountById;
import com.administrapochi.repositories.ReportReadRepository.DatedId;
import com.administrapochi.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 16);
    private static final LocalDate END = START.plusDays(6);
    private static final int CLOSING_ID = 8;
    private static final int OWNER_ID = 12;

    @Mock private ReportReadRepository repository;
    @Mock private UserRepository users;
    private ReportsService reports;

    @BeforeEach
    void setUp() {
        reports = new ReportsService(repository, new ReportAccess(users),
                new WeeklyReportAssembler(Mappers.getMapper(ReportsMapper.class)));
    }

    @ParameterizedTest
    @CsvSource({"ADMIN,OPEN,true", "ADMIN,ADMIN_CLOSED,false", "ADMIN,OWNER_CLOSED,false",
            "OWNER,ADMIN_CLOSED,false", "OWNER,OWNER_CLOSED,false"})
    void returnsVisibleWeeks(UserRole role, WeeklyClosingStatus status, boolean preview) {
        actor(role);
        week(role == UserRole.OWNER ? 2 : OWNER_ID, status);
        var result = reports.weekly(role == UserRole.OWNER ? null : OWNER_ID, 2026, 47);
        assertThat(result.closing().status()).isEqualTo(status);
        assertThat(result.closing().preview()).isEqualTo(preview);
        assertThat(result.closing().closingComment()).isEqualTo("Cash delivered");
        assertThat(result.week().startDate()).isEqualTo(START);
        assertThat(result.week().endDate()).isEqualTo(END);
    }

    @Test
    void ownerCannotViewOpenOrSelectAnotherOwner() {
        actor(UserRole.OWNER);
        week(2, WeeklyClosingStatus.OPEN);
        assertThatThrownBy(() -> reports.weekly(null, 2026, 47)).isInstanceOf(ReportAccessDeniedException.class);
        assertThatThrownBy(() -> reports.weekly(OWNER_ID, 2026, 47)).isInstanceOf(ReportAccessDeniedException.class);
        verify(repository, never()).findSnapshotPochis(any());
        verify(repository, never()).findWeek(eq(OWNER_ID), any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource(value = {"2026-11-17,2026-11-22", "2026-11-16,2026-11-21", "2026-11-16,2026-11-29",
            "2026-11-16,2026-11-20", "2026-11-16,2026-11-15", "null,2026-11-22", "2026-11-16,null"},
            nullValues = "null")
    void rejectsInvalidRange(LocalDate start, LocalDate end) {
        assertThatThrownBy(() -> reports.weeklyByRange(null, start, end))
                .isInstanceOf(InvalidWeeklyClosingException.class);
        verifyNoInteractions(users, repository);
    }

    @ParameterizedTest
    @CsvSource({"2025,53", "2026,0", "2026,54", "0,1", "10000,1"})
    void rejectsInvalidIsoWeek(int year, int week) {
        assertThatThrownBy(() -> reports.weekly(null, year, week)).isInstanceOf(InvalidWeeklyClosingException.class);
        verifyNoInteractions(users, repository);
    }

    @Test
    void returnsNotFoundOrAmbiguousWeekWithoutReadingMovements() {
        actor(UserRole.ADMIN);
        var limit = PageRequest.of(0, 2);
        assertThatThrownBy(() -> reports.weeklyByRange(null, START, END))
                .isInstanceOf(WeeklyClosingNotFoundException.class);
        when(repository.findWeek(null, START, END, limit)).thenReturn(
                List.of(closing(OWNER_ID, WeeklyClosingStatus.OPEN), closing(13, WeeklyClosingStatus.OPEN)));
        assertThatThrownBy(() -> reports.weeklyByRange(null, START, END))
                .isInstanceOf(InvalidWeeklyClosingException.class).hasMessageContaining("ownerId");
        verify(repository, never()).findSnapshotPochis(any());
    }

    @Test
    void aggregatesHistoricalRecordsAndShowsTheirTraceabilityWithoutCountingTwice() {
        actor(UserRole.ADMIN);
        week(OWNER_ID, WeeklyClosingStatus.OPEN);
        Pochi first = Pochi.builder().id(1).economicNumber("82").build();
        Pochi second = Pochi.builder().id(2).economicNumber("91").build();
        var ids = List.of(2, 1);
        when(repository.findSnapshotPochis(CLOSING_ID)).thenReturn(List.of(second, first));
        Driver driver = Driver.builder().id(7).firstName("Roberto").firstLastName("Lopez").build();
        var morning = assignment(11, first, driver, START, Shift.MORNING);
        var afternoon = assignment(12, first, driver, START, Shift.AFTERNOON);
        var secondShift = assignment(13, second, driver, START, Shift.AFTERNOON);
        var missingSettlement = assignment(14, first, driver, START.plusDays(4), Shift.MORNING);
        when(repository.findAssignments(ids, START, END)).thenReturn(
                List.of(afternoon, missingSettlement, morning, secondShift));
        when(repository.findDailyStatuses(ids, START, END)).thenReturn(List.of(
                daily(1, first, 1, PochiDailyStatusType.NOT_WORKED, "Workshop"),
                daily(2, first, 2, PochiDailyStatusType.DAY_OFF, "Holiday"),
                daily(3, first, 3, PochiDailyStatusType.UNION_REST, null)));

        var settlement = settlement(21, first, START, "200.00", "150.00", "Shortfall", morning, afternoon);
        var equal = settlement(22, second, START, "0.30", "0.30", null, secondShift);
        when(repository.findSettlements(ids, START, END)).thenReturn(List.of(settlement, equal));
        var charge = AdministrationCharge.builder().id(31).pochi(first).chargeDate(START)
                .amount(money("30.10")).suggestedAmount(money("39.99")).note("Recorded override").build();
        when(repository.findCharges(ids, START, END)).thenReturn(List.of(charge));
        var type = ExpenseType.builder().id(40).code("REPAIR").name("Repair").build();
        var expense = Expense.builder().id(41).pochi(first).expenseDate(START).expenseType(type)
                .description("Tire").status(ExpenseStatus.ACTIVE).amount(money("10.20")).build();
        when(repository.findExpenses(ids, START, END)).thenReturn(List.of(expense));
        when(repository.findGlobalExpenses(START, END)).thenReturn(List.of(dated(42, START)));

        var debt = debt(51, driver, settlement, START, "50.00", DriverDebtStatus.PARTIALLY_PAID);
        when(repository.findNewDebts(CLOSING_ID)).thenReturn(List.of(debt));
        when(repository.sumPaidByDebt(List.of(51))).thenReturn(List.of(amount(51, "10.00")));
        var olderDate = START.minusWeeks(2);
        var origin = settlement(23, first, olderDate, "400.00", "100.00", "Older shortfall",
                assignment(15, first, driver, olderDate, Shift.AFTERNOON));
        var olderDebt = debt(52, driver, origin, olderDate, "300.00", DriverDebtStatus.PARTIALLY_PAID);
        var payment = DriverDebtPayment.builder().id(61).driverDebt(olderDebt).paymentDate(END)
                .amount(money("100.01")).notes("Cash").build();
        when(repository.findPayments(CLOSING_ID)).thenReturn(List.of(payment));
        when(repository.findUnresolvedPayments(START, END)).thenReturn(List.of(dated(62, END)));

        var report = reports.weeklyByRange(OWNER_ID, START, END);
        assertThat(report.pochis()).extracting(p -> p.pochiId()).containsExactly(1, 2);
        var pochi = report.pochis().getFirst();
        assertThat(pochi.days()).extracting(PochiDailyReportResponse::date)
                .containsExactlyElementsOf(START.datesUntil(END.plusDays(1)).toList());
        assertThat(pochi.days()).extracting(PochiDailyReportResponse::dailyStatus)
                .containsExactly("WORKED", "NOT_WORKED", "DAY_OFF", "UNION_REST", "WORKED", null, null);
        assertThat(pochi.days().get(1).statusComment()).isEqualTo("Workshop");
        assertThat(pochi.days().get(1).assignments()).isEmpty();
        assertThat(pochi.days().getFirst().assignments()).extracting(a -> a.shift())
                .containsExactly(Shift.MORNING, Shift.AFTERNOON);
        var worked = pochi.days().getFirst();
        assertThat(worked.assignments().getFirst().driver().driverName()).isEqualTo("Roberto Lopez");
        assertThat(worked.assignments().getFirst().settlement().difference()).isEqualByComparingTo("-50.00");
        assertThat(worked.assignments().getFirst().settlement().adjustmentReason()).isEqualTo("Shortfall");
        assertThat(report.pochis().getLast().days().getFirst().assignments().getFirst().settlement().difference())
                .isEqualByComparingTo("0.00");
        assertThat(worked.administrationCharge().amount()).isEqualByComparingTo("30.10");
        assertThat(worked.administrationCharge().note()).isEqualTo("Recorded override");
        assertThat(pochi.expenses()).singleElement().satisfies(e -> {
            assertThat(e.expenseId()).isEqualTo(41);
            assertThat(e.description()).isEqualTo("Tire");
            assertThat(e.expenseTypeCode()).isEqualTo("REPAIR");
        });
        assertThat(pochi.debtsGenerated()).singleElement().satisfies(d -> {
            assertThat(d.origin().settlementId()).isEqualTo(21);
            assertThat(d.origin().originShifts()).containsExactly(Shift.MORNING, Shift.AFTERNOON);
            assertThat(d.currentBalance()).isEqualByComparingTo("40.00");
        });
        assertThat(pochi.debtPayments()).singleElement().satisfies(p -> {
            assertThat(p.paymentDate()).isEqualTo(END);
            assertThat(p.driver().driverId()).isEqualTo(7);
            assertThat(p.debtId()).isEqualTo(52);
            assertThat(p.origin().settlementId()).isEqualTo(23);
            assertThat(p.origin().pochiIdentifier()).isEqualTo("82");
            assertThat(p.origin().originDate()).isEqualTo(olderDate);
            assertThat(p.origin().shift()).isEqualTo(Shift.AFTERNOON);
            assertThat(p.balanceBeforePayment()).isNull();
            assertThat(p.balanceAfterPayment()).isNull();
        });
        assertThat(report.summary().expectedSettlements()).isEqualByComparingTo("200.30");
        assertThat(report.summary().acceptedSettlements()).isEqualByComparingTo("150.30");
        assertThat(report.summary().settlementDifference()).isEqualByComparingTo("-50.00");
        assertThat(report.summary().newDriverDebts()).isEqualByComparingTo("50.00");
        assertThat(report.summary().grossReceived()).isEqualByComparingTo("250.31");
        assertThat(report.summary().totalToOwner()).isEqualByComparingTo("210.01");
        assertThat(pochi.summary().totalToOwner().add(report.pochis().getLast().summary().totalToOwner()))
                .isEqualByComparingTo(report.summary().totalToOwner());
        assertThat(report.warnings()).extracting(ReportWarningResponse::code).contains(
                "DEBT_PAYMENT_OWNER_UNRESOLVED", "EXPENSE_WITHOUT_POCHI_EXCLUDED",
                "ASSIGNMENT_WITHOUT_SETTLEMENT", "MISSING_ADMINISTRATION_CHARGE", "MISSING_DAILY_STATUS");
        verify(repository).findWeek(OWNER_ID, START, END, PageRequest.of(0, 2));
        verify(repository).findSnapshotPochis(CLOSING_ID);
        verify(repository).findAssignments(ids, START, END);
        verify(repository).findDailyStatuses(ids, START, END);
        verify(repository).findSettlements(ids, START, END);
        verify(repository).findCharges(ids, START, END);
        verify(repository).findExpenses(ids, START, END);
        verify(repository).findNewDebts(CLOSING_ID);
        verify(repository).findPayments(CLOSING_ID);
        verify(repository).sumPaidByDebt(List.of(51));
        verify(repository).findGlobalExpenses(START, END);
        verify(repository).findUnresolvedPayments(START, END);
        verifyNoMoreInteractions(repository);
        assertThat(settlement.getAcceptedAmount()).isEqualByComparingTo("150.00");
        assertThat(debt.getStatus()).isEqualTo(DriverDebtStatus.PARTIALLY_PAID);
    }

    @Test
    void ownerReadsOnlyItsSnapshotAndDoesNotAccessUnscopedWarnings() {
        actor(UserRole.OWNER);
        week(2, WeeklyClosingStatus.ADMIN_CLOSED);
        when(repository.findSnapshotPochis(CLOSING_ID))
                .thenReturn(List.of(Pochi.builder().id(1).economicNumber("82").build()));
        assertThat(reports.weekly(null, 2026, 47).pochis()).extracting(p -> p.pochiId()).containsExactly(1);
        verify(repository).findExpenses(List.of(1), START, END);
        verify(repository, never()).findUnresolvedPayments(any(), any());
        verify(repository, never()).findGlobalExpenses(any(), any());
    }

    @Test
    void historyAndPendingReturnPaginatedSummariesWithoutLoadingDailyDetails() {
        actor(UserRole.OWNER);
        var page = PageRequest.of(0, 5);
        var sorted = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "weekStart", "id"));
        var closed = closing(2, WeeklyClosingStatus.ADMIN_CLOSED);
        var ownerStatuses = List.of(WeeklyClosingStatus.ADMIN_CLOSED, WeeklyClosingStatus.OWNER_CLOSED);
        when(repository.findHistory(2, ownerStatuses, 2026, START, END, sorted))
                .thenReturn(new PageImpl<>(List.of(closed), sorted, 1));
        when(repository.findHistory(2, List.of(WeeklyClosingStatus.ADMIN_CLOSED), null, null, null, sorted))
                .thenReturn(new PageImpl<>(List.of(closed), sorted, 1));
        when(repository.sumAcceptedSettlements(List.of(CLOSING_ID))).thenReturn(List.of(amount(CLOSING_ID, "90.55")));
        when(repository.sumPayments(List.of(CLOSING_ID))).thenReturn(List.of(amount(CLOSING_ID, "25.93")));
        when(repository.sumCharges(List.of(CLOSING_ID))).thenReturn(List.of(amount(CLOSING_ID, "10.20")));
        when(repository.sumExpenses(List.of(CLOSING_ID))).thenReturn(List.of(amount(CLOSING_ID, "1.25")));

        var history = reports.history(null, 2026, null, START, END, page);
        assertThat(history.getTotalElements()).isEqualTo(1);
        assertThat(history.getContent().getFirst().totalToOwner()).isEqualByComparingTo("105.03");
        assertThat(reports.pendingOwnerClose(null, page).getContent()).isEqualTo(history.getContent());
        assertThatThrownBy(() -> reports.history(null, null, WeeklyClosingStatus.OPEN, null, null, page))
                .isInstanceOf(ReportAccessDeniedException.class);
        verify(repository, never()).findSnapshotPochis(any());
        verify(repository, never()).findAssignments(any(), any(), any());
    }

    private void actor(UserRole role) {
        when(users.findById(2)).thenReturn(Optional.of(User.builder().id(2).role(role).build()));
    }

    private void week(Integer ownerId, WeeklyClosingStatus status) {
        when(repository.findWeek(ownerId, START, END, PageRequest.of(0, 2)))
                .thenReturn(List.of(closing(ownerId, status)));
    }

    private static WeeklyClosing closing(Integer ownerId, WeeklyClosingStatus status) {
        return WeeklyClosing.builder().id(CLOSING_ID).owner(User.builder().id(ownerId).build()).status(status)
                .weekYear(2026).weekNumber(47).weekStart(START).weekEnd(END).closingComment("Cash delivered").build();
    }

    private static PochiAssignment assignment(int id, Pochi pochi, Driver driver, LocalDate date, Shift shift) {
        return PochiAssignment.builder().id(id).pochi(pochi).driver(driver).assignmentDate(date).shift(shift).build();
    }

    private static PochiDailyStatus daily(int id, Pochi pochi, int offset, PochiDailyStatusType status, String comment) {
        return PochiDailyStatus.builder().id(id).pochi(pochi).statusDate(START.plusDays(offset))
                .status(status).comment(comment).build();
    }

    private static Settlement settlement(int id, Pochi pochi, LocalDate date, String expected, String accepted,
                                         String reason, PochiAssignment... assignments) {
        return Settlement.builder().id(id).pochi(pochi).workDate(date).expectedAmount(money(expected))
                .acceptedAmount(money(accepted)).adjustmentReason(reason).pochiAssignments(Set.of(assignments)).build();
    }

    private static DriverDebt debt(int id, Driver driver, Settlement origin, LocalDate date, String amount,
                                   DriverDebtStatus status) {
        return DriverDebt.builder().id(id).driver(driver).settlement(origin).debtDate(date)
                .amount(money(amount)).status(status).build();
    }

    private static AmountById amount(int id, String value) {
        return new AmountById() {
            public Integer getRecordId() { return id; }
            public BigDecimal getAmount() { return money(value); }
        };
    }

    private static DatedId dated(int id, LocalDate date) {
        return new DatedId() {
            public Integer getId() { return id; }
            public LocalDate getDate() { return date; }
        };
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}

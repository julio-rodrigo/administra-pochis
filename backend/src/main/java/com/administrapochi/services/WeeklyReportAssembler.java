package com.administrapochi.services;

import com.administrapochi.models.dtos.reports.WeeklyReportResponse;
import com.administrapochi.models.dtos.reports.WeeklyReportResponse.*;
import com.administrapochi.models.entities.*;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.mappers.ReportsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class WeeklyReportAssembler {

    static final BigDecimal ZERO = new BigDecimal("0.00");
    private final ReportsMapper mapper;

    record Sources(List<Pochi> snapshot, List<PochiAssignment> assignments, List<PochiDailyStatus> statuses,
                   List<Settlement> settlements, List<AdministrationCharge> charges, List<Expense> expenses,
                   List<DriverDebt> debts, List<DriverDebtPayment> payments, Map<Integer, BigDecimal> paidByDebt) {}

    WeeklyReportResponse assemble(WeeklyClosing closing, boolean preview, Sources data,
                                  List<ReportWarningResponse> exclusions) {
        List<ReportWarningResponse> warnings = new ArrayList<>(exclusions);
        Map<Integer, Pochi> pochis = new HashMap<>();
        data.snapshot().forEach(p -> pochis.put(p.getId(), p));
        Set<Integer> operationalPochis = Set.copyOf(pochis.keySet());
        // Money can be recovered for a historical Pochi absent from the current snapshot.
        data.debts().forEach(d -> addOriginPochi(pochis, d));
        data.payments().forEach(p -> addOriginPochi(pochis, p.getDriverDebt()));
        var assignments = group(data.assignments(), a -> day(a.getPochi(), a.getAssignmentDate()));
        var statuses = group(data.statuses(), s -> day(s.getPochi(), s.getStatusDate()));
        var charges = group(data.charges(), c -> day(c.getPochi(), c.getChargeDate()));
        var chargesByPochi = group(data.charges(), c -> c.getPochi().getId());
        var settlements = group(data.settlements(), s -> s.getPochi().getId());
        var expenses = group(data.expenses(), e -> e.getPochi().getId());
        var debts = group(data.debts(), d -> d.getSettlement().getPochi().getId());
        var payments = group(data.payments(), p -> p.getDriverDebt().getSettlement().getPochi().getId());
        Map<Integer, Settlement> settlementByAssignment = indexSettlements(data.settlements(), warnings);

        List<PochiWeeklyReportResponse> details = pochis.values().stream()
                .sorted(Comparator.comparing(Pochi::getEconomicNumber).thenComparing(Pochi::getId))
                .map(pochi -> {
                    Integer id = pochi.getId();
                    var days = closing.getWeekStart().datesUntil(closing.getWeekEnd().plusDays(1))
                            .map(date -> {
                                Day key = new Day(id, date);
                                return daily(key, operationalPochis.contains(id), rows(assignments, key),
                                        rows(statuses, key), rows(charges, key), settlementByAssignment, warnings);
                            }).toList();
                    var pochiExpenses = rows(expenses, id).stream()
                            .sorted(Comparator.comparing(Expense::getExpenseDate).thenComparing(Expense::getId))
                            .map(mapper::toExpense).toList();
                    var pochiDebts = rows(debts, id).stream()
                            .sorted(Comparator.comparing(DriverDebt::getDebtDate).thenComparing(DriverDebt::getId))
                            .map(d -> mapper.toDebt(d, d.getAmount().subtract(data.paidByDebt()
                                    .getOrDefault(d.getId(), ZERO)))).toList();
                    var pochiPayments = rows(payments, id).stream().sorted(Comparator
                                    .comparing(DriverDebtPayment::getPaymentDate).thenComparing(DriverDebtPayment::getId))
                            .map(mapper::toPayment).toList();
                    var pochiSettlements = rows(settlements, id);
                    var summary = totals(sum(pochiSettlements, Settlement::getExpectedAmount),
                            sum(pochiSettlements, Settlement::getAcceptedAmount),
                            sum(pochiPayments, DebtPaymentReportResponse::amount),
                            sum(rows(chargesByPochi, id), AdministrationCharge::getAmount),
                            sum(pochiExpenses, ExpenseReportResponse::amount), sum(pochiDebts, DriverDebtReportResponse::originalAmount));
                    return new PochiWeeklyReportResponse(id, pochi.getEconomicNumber(), days,
                            pochiExpenses, pochiDebts, pochiPayments, summary);
                }).toList();
        var summaries = details.stream().map(PochiWeeklyReportResponse::summary).toList();
        var overall = totals(sum(summaries, FinancialSummaryResponse::expectedSettlements),
                sum(summaries, FinancialSummaryResponse::acceptedSettlements),
                sum(summaries, FinancialSummaryResponse::debtPaymentsReceived),
                sum(summaries, FinancialSummaryResponse::administrationCharges),
                sum(summaries, FinancialSummaryResponse::expenses), sum(summaries, FinancialSummaryResponse::newDriverDebts));
        return new WeeklyReportResponse(mapper.toWeek(closing), mapper.toClosing(closing, preview),
                details, overall, List.copyOf(warnings));
    }

    private PochiDailyReportResponse daily(Day day, boolean operational, List<PochiAssignment> assignments,
                                           List<PochiDailyStatus> statuses, List<AdministrationCharge> charges,
                                           Map<Integer, Settlement> settlements, List<ReportWarningResponse> warnings) {
        var status = statuses.stream().min(Comparator.comparing(PochiDailyStatus::getId)).orElse(null);
        var charge = charges.stream().min(Comparator.comparing(AdministrationCharge::getId)).orElse(null);
        if (statuses.size() > 1) {
            warn(warnings, "DUPLICATE_SOURCE_DATA", "Multiple daily statuses were recorded.",
                    day, null, "PochiDailyStatus", status.getId());
        }
        if (charges.size() > 1) {
            warn(warnings, "DUPLICATE_SOURCE_DATA", "Multiple administration charges were recorded.",
                    day, null, "AdministrationCharge", charge.getId());
        }
        boolean worked = status == null && !assignments.isEmpty();
        if (operational && status == null && !worked) {
            warn(warnings, "MISSING_DAILY_STATUS", "No daily status or assignment was recorded.",
                    day, null, "PochiDailyStatus", null);
        }
        var result = new ArrayList<AssignmentReportResponse>();
        var seen = EnumSet.noneOf(Shift.class);
        for (var assignment : assignments.stream().sorted(Comparator.comparing(PochiAssignment::getShift)
                .thenComparing(PochiAssignment::getId)).toList()) {
            var settlement = settlements.get(assignment.getId());
            if (!seen.add(assignment.getShift())) {
                warn(warnings, "DUPLICATE_SOURCE_DATA", "Multiple assignments share this shift.",
                        day, assignment.getShift(), "PochiAssignment", assignment.getId());
            }
            if (worked && settlement == null) {
                warn(warnings, "ASSIGNMENT_WITHOUT_SETTLEMENT", "Worked assignment has no settlement.",
                        day, assignment.getShift(), "PochiAssignment", assignment.getId());
            }
            result.add(mapper.toAssignment(assignment, settlement));
        }
        if (worked && charge == null && result.stream().allMatch(a -> a.settlement() != null)) {
            warn(warnings, "MISSING_ADMINISTRATION_CHARGE", "Settled work has no administration charge.",
                    day, null, "AdministrationCharge", null);
        }
        return new PochiDailyReportResponse(day.date(), day.date().getDayOfWeek(),
                status != null ? status.getStatus().name() : worked ? "WORKED" : null,
                status == null ? null : status.getComment(), List.copyOf(result), mapper.toCharge(charge));
    }

    private Map<Integer, Settlement> indexSettlements(List<Settlement> settlements, List<ReportWarningResponse> warnings) {
        Map<Integer, Settlement> byAssignment = new HashMap<>();
        for (var settlement : settlements.stream().sorted(Comparator.comparing(Settlement::getId)).toList()) {
            Day day = day(settlement.getPochi(), settlement.getWorkDate());
            if (settlement.getAcceptedAmount().compareTo(settlement.getExpectedAmount()) != 0
                    && (settlement.getAdjustmentReason() == null || settlement.getAdjustmentReason().isBlank())) {
                warn(warnings, "SETTLEMENT_ADJUSTMENT_WITHOUT_REASON", "Settlement adjustment has no reason.",
                        day, null, "Settlement", settlement.getId());
            }
            if (settlement.getPochiAssignments().isEmpty()) {
                warn(warnings, "SETTLEMENT_WITHOUT_ASSIGNMENT", "Recorded settlement is not linked to an assignment.",
                        day, null, "Settlement", settlement.getId());
            }
            settlement.getPochiAssignments().stream().sorted(Comparator.comparing(PochiAssignment::getId)).forEach(a -> {
                if (byAssignment.putIfAbsent(a.getId(), settlement) != null) {
                    warn(warnings, "DUPLICATE_SOURCE_DATA", "Multiple settlements are linked to this assignment.",
                            day, a.getShift(), "Settlement", settlement.getId());
                }
            });
        }
        return byAssignment;
    }

    private static void warn(List<ReportWarningResponse> warnings, String code, String message, Day day,
                             Shift shift, String source, Integer id) {
        warnings.add(new ReportWarningResponse(code, message, day.pochiId(), day.date(), shift, source, id));
    }

    private static void addOriginPochi(Map<Integer, Pochi> pochis, DriverDebt debt) {
        Pochi pochi = debt.getSettlement().getPochi();
        pochis.putIfAbsent(pochi.getId(), pochi);
    }

    private static Day day(Pochi pochi, LocalDate date) {
        return new Day(pochi.getId(), date);
    }

    private static <K, T> Map<K, List<T>> group(List<T> records, Function<T, K> key) {
        return records.stream().collect(Collectors.groupingBy(key));
    }

    private static <K, T> List<T> rows(Map<K, List<T>> groups, K key) {
        return groups.getOrDefault(key, List.of());
    }

    private static <T> BigDecimal sum(List<T> values, Function<T, BigDecimal> amount) {
        return values.stream().map(amount).reduce(ZERO, BigDecimal::add);
    }

    private static FinancialSummaryResponse totals(BigDecimal expected, BigDecimal accepted, BigDecimal payments,
                                                   BigDecimal charges, BigDecimal expenses, BigDecimal debts) {
        BigDecimal gross = accepted.add(payments);
        return new FinancialSummaryResponse(expected, accepted, accepted.subtract(expected), payments,
                gross, charges, expenses, debts, gross.subtract(charges).subtract(expenses));
    }

    private record Day(Integer pochiId, LocalDate date) {}
}

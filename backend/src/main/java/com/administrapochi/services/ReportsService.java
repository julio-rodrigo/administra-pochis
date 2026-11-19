package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidWeeklyClosingException;
import com.administrapochi.exceptions.WeeklyClosingNotFoundException;
import com.administrapochi.models.dtos.reports.WeeklyReportHistoryResponse;
import com.administrapochi.models.dtos.reports.WeeklyReportResponse;
import com.administrapochi.models.dtos.reports.WeeklyReportResponse.ReportWarningResponse;
import com.administrapochi.models.entities.DriverDebt;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.WeeklyClosing;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import com.administrapochi.repositories.ReportReadRepository;
import com.administrapochi.repositories.ReportReadRepository.AmountById;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.administrapochi.services.WeeklyReportAssembler.ZERO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportsService {

    private static final Set<String> HISTORY_SORT_FIELDS =
            Set.of("id", "ownerId", "weekYear", "weekNumber", "weekStart", "weekEnd", "status");

    private final ReportReadRepository repository;
    private final ReportAccess access;
    private final WeeklyReportAssembler assembler;

    public WeeklyReportResponse weekly(Integer ownerId, Integer weekYear, Integer weekNumber) {
        LocalDate start = IsoWeeks.start(weekYear, weekNumber);
        return detail(ownerId, start, start.plusDays(6));
    }

    public WeeklyReportResponse weeklyByRange(Integer ownerId, LocalDate start, LocalDate end) {
        if (start == null || end == null || start.getDayOfWeek() != DayOfWeek.MONDAY
                || end.getDayOfWeek() != DayOfWeek.SUNDAY || ChronoUnit.DAYS.between(start, end) != 6) {
            throw new InvalidWeeklyClosingException("A range of exactly one week (Monday through Sunday) is required.");
        }
        return detail(ownerId, start, end);
    }

    private WeeklyReportResponse detail(Integer requestedOwnerId, LocalDate start, LocalDate end) {
        var scope = access.resolve(requestedOwnerId);
        var candidates = repository.findWeek(scope.ownerId(), start, end, PageRequest.of(0, 2));
        if (candidates.isEmpty()) {
            throw new WeeklyClosingNotFoundException(start, end);
        }
        if (candidates.size() > 1) {
            throw new InvalidWeeklyClosingException("Specify ownerId when multiple Owners have a Weekly Closing.");
        }

        var closing = candidates.getFirst();
        scope.requireVisible(closing.getStatus());
        Integer id = closing.getId();
        var weekStart = closing.getWeekStart();
        var weekEnd = closing.getWeekEnd();
        var snapshot = repository.findSnapshotPochis(id);
        var pochiIds = snapshot.stream().map(Pochi::getId).distinct().toList();
        var debts = repository.findNewDebts(id);
        var debtIds = debts.stream().map(DriverDebt::getId).toList();
        var sources = new WeeklyReportAssembler.Sources(snapshot,
                pochiIds.isEmpty() ? List.of() : repository.findAssignments(pochiIds, weekStart, weekEnd),
                pochiIds.isEmpty() ? List.of() : repository.findDailyStatuses(pochiIds, weekStart, weekEnd),
                pochiIds.isEmpty() ? List.of() : repository.findSettlements(pochiIds, weekStart, weekEnd),
                pochiIds.isEmpty() ? List.of() : repository.findCharges(pochiIds, weekStart, weekEnd),
                pochiIds.isEmpty() ? List.of() : repository.findExpenses(pochiIds, weekStart, weekEnd),
                debts, repository.findPayments(id),
                debtIds.isEmpty() ? Map.of() : amounts(repository.sumPaidByDebt(debtIds)));

        boolean preview = closing.getStatus() == WeeklyClosingStatus.OPEN;
        List<ReportWarningResponse> exclusions = new ArrayList<>();
        if (scope.admin()) {
            repository.findUnresolvedPayments(weekStart, weekEnd).forEach(p -> exclusions.add(
                    new ReportWarningResponse("DEBT_PAYMENT_OWNER_UNRESOLVED",
                            "Debt payment excluded because its Owner cannot be determined.",
                            null, p.getDate(), null, "DriverDebtPayment", p.getId())));
            if (preview) {
                repository.findGlobalExpenses(weekStart, weekEnd).forEach(e -> exclusions.add(
                        new ReportWarningResponse("EXPENSE_WITHOUT_POCHI_EXCLUDED",
                                "Expense excluded because it has no Pochi.",
                                null, e.getDate(), null, "Expense", e.getId())));
            }
        }
        return assembler.assemble(closing, preview, sources, exclusions);
    }

    public Page<WeeklyReportHistoryResponse> history(Integer ownerId, Integer year, WeeklyClosingStatus status,
                                                     LocalDate from, LocalDate to, Pageable pageable) {
        var scope = access.resolve(ownerId);
        if (year != null) {
            IsoWeeks.validateYear(year);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidWeeklyClosingException("startDate must not be after endDate.");
        }
        if (status != null) {
            scope.requireVisible(status);
        }
        var statuses = status == null ? scope.visibleStatuses() : List.of(status);
        return list(scope.ownerId(), statuses, year, from, to, pageable);
    }

    public Page<WeeklyReportHistoryResponse> pendingOwnerClose(Integer ownerId, Pageable pageable) {
        var scope = access.resolve(ownerId);
        return list(scope.ownerId(), List.of(WeeklyClosingStatus.ADMIN_CLOSED), null, null, null, pageable);
    }

    private Page<WeeklyReportHistoryResponse> list(Integer ownerId, List<WeeklyClosingStatus> statuses,
                                                   Integer year, LocalDate from, LocalDate to, Pageable pageable) {
        var closings = repository.findHistory(ownerId, statuses, year, from, to, historyPage(pageable));
        if (closings.isEmpty()) {
            return closings.map(c -> historyEntry(c, ZERO, ZERO, ZERO, ZERO));
        }
        var ids = closings.stream().map(WeeklyClosing::getId).toList();
        var accepted = amounts(repository.sumAcceptedSettlements(ids));
        var payments = amounts(repository.sumPayments(ids));
        var charges = amounts(repository.sumCharges(ids));
        var expenses = amounts(repository.sumExpenses(ids));
        return closings.map(c -> historyEntry(c, accepted.getOrDefault(c.getId(), ZERO),
                payments.getOrDefault(c.getId(), ZERO), charges.getOrDefault(c.getId(), ZERO),
                expenses.getOrDefault(c.getId(), ZERO)));
    }

    private WeeklyReportHistoryResponse historyEntry(WeeklyClosing c, BigDecimal accepted, BigDecimal payments,
                                                      BigDecimal charges, BigDecimal expenses) {
        return new WeeklyReportHistoryResponse(c.getId(), c.getOwner().getId(), c.getWeekYear(), c.getWeekNumber(),
                c.getWeekStart(), c.getWeekEnd(), c.getStatus(), c.getClosingComment(), c.getAdminClosedAt(),
                c.getOwnerClosedAt(), accepted, payments, charges, expenses,
                accepted.add(payments).subtract(charges).subtract(expenses));
    }

    private Pageable historyPage(Pageable pageable) {
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSortOr(Sort.by(Sort.Direction.DESC, "weekStart"))) {
            String field = switch (order.getProperty()) {
                case "startDate" -> "weekStart";
                case "endDate" -> "weekEnd";
                case "weeklyClosingId" -> "id";
                default -> order.getProperty();
            };
            if (!HISTORY_SORT_FIELDS.contains(field)) {
                throw new InvalidWeeklyClosingException("Unsupported weekly report sort field: " + order.getProperty());
            }
            orders.add(order.withProperty(field.equals("ownerId") ? "owner.id" : field));
        }
        if (orders.stream().noneMatch(o -> o.getProperty().equals("id"))) {
            orders.add(Sort.Order.desc("id"));
        }
        return PageRequest.of(pageable.isPaged() ? pageable.getPageNumber() : 0,
                pageable.isPaged() ? pageable.getPageSize() : 20, Sort.by(orders));
    }

    private static Map<Integer, BigDecimal> amounts(List<AmountById> amounts) {
        return amounts.stream().collect(Collectors.toMap(AmountById::getRecordId, AmountById::getAmount));
    }
}

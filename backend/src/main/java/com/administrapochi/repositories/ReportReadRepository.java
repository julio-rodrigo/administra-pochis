package com.administrapochi.repositories;

import com.administrapochi.models.entities.*;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ReportReadRepository extends Repository<WeeklyClosing, Integer> {

    String CLOSING_FILTERS = """
            WHERE (:ownerId IS NULL OR c.owner.id = :ownerId)
              AND c.status IN :statuses
              AND (:weekYear IS NULL OR c.weekYear = :weekYear)
              AND (:startDate IS NULL OR c.weekStart >= :startDate)
              AND (:endDate IS NULL OR c.weekEnd <= :endDate)
            """;

    String ORIGIN_OWNERS = """
            SELECT s.weeklyClosing.owner.id FROM WeeklyClosingPochi s
            WHERE s.pochi.id = origin.pochi.id
              AND origin.workDate BETWEEN s.weeklyClosing.weekStart AND s.weeklyClosing.weekEnd
            """;

    // A recorded origin-week snapshot takes precedence over current ownership.
    String ORIGIN_SCOPE = " AND ((c.owner.id IN (" + ORIGIN_OWNERS
            + ") AND NOT EXISTS (" + ORIGIN_OWNERS + " AND s.weeklyClosing.owner.id <> c.owner.id))"
            + " OR (NOT EXISTS (" + ORIGIN_OWNERS + ") AND origin.pochi.owner.id = c.owner.id))";

    @Query("""
            SELECT c FROM WeeklyClosing c
            WHERE c.weekStart = :startDate AND c.weekEnd = :endDate
              AND (:ownerId IS NULL OR c.owner.id = :ownerId)
            ORDER BY c.id
            """)
    List<WeeklyClosing> findWeek(Integer ownerId, LocalDate startDate, LocalDate endDate, Pageable limit);

    @Query(value = "SELECT c FROM WeeklyClosing c " + CLOSING_FILTERS,
            countQuery = "SELECT COUNT(c) FROM WeeklyClosing c " + CLOSING_FILTERS)
    Page<WeeklyClosing> findHistory(Integer ownerId, Collection<WeeklyClosingStatus> statuses,
                                    Integer weekYear, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT s.pochi FROM WeeklyClosingPochi s WHERE s.weeklyClosing.id = :closingId")
    List<Pochi> findSnapshotPochis(Integer closingId);

    @Query("""
            SELECT a FROM PochiAssignment a JOIN FETCH a.driver
            WHERE a.pochi.id IN :pochiIds AND a.assignmentDate BETWEEN :startDate AND :endDate
            """)
    List<PochiAssignment> findAssignments(Collection<Integer> pochiIds, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT d FROM PochiDailyStatus d
            WHERE d.pochi.id IN :pochiIds AND d.statusDate BETWEEN :startDate AND :endDate
            """)
    List<PochiDailyStatus> findDailyStatuses(Collection<Integer> pochiIds, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT s FROM Settlement s LEFT JOIN FETCH s.pochiAssignments
            WHERE s.pochi.id IN :pochiIds AND s.workDate BETWEEN :startDate AND :endDate
            """)
    List<Settlement> findSettlements(Collection<Integer> pochiIds, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT a FROM AdministrationCharge a
            WHERE a.pochi.id IN :pochiIds AND a.chargeDate BETWEEN :startDate AND :endDate
            """)
    List<AdministrationCharge> findCharges(Collection<Integer> pochiIds, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT e FROM Expense e JOIN FETCH e.expenseType JOIN FETCH e.pochi
            WHERE e.pochi.id IN :pochiIds AND e.expenseDate BETWEEN :startDate AND :endDate AND e.status = ACTIVE
            """)
    List<Expense> findExpenses(Collection<Integer> pochiIds, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT d FROM DriverDebt d JOIN FETCH d.driver JOIN FETCH d.settlement origin
            JOIN FETCH origin.pochi LEFT JOIN FETCH origin.pochiAssignments
            , WeeklyClosing c
            WHERE c.id = :closingId AND d.debtDate BETWEEN c.weekStart AND c.weekEnd AND d.status <> CANCELLED
            """ + ORIGIN_SCOPE)
    List<DriverDebt> findNewDebts(Integer closingId);

    @Query("""
            SELECT p FROM DriverDebtPayment p JOIN FETCH p.driverDebt d JOIN FETCH d.driver
            JOIN FETCH d.settlement origin JOIN FETCH origin.pochi LEFT JOIN FETCH origin.pochiAssignments
            , WeeklyClosing c
            WHERE c.id = :closingId AND p.paymentDate BETWEEN c.weekStart AND c.weekEnd AND d.status <> CANCELLED
            """ + ORIGIN_SCOPE)
    List<DriverDebtPayment> findPayments(Integer closingId);

    @Query("""
            SELECT p.driverDebt.id AS recordId, SUM(p.amount) AS amount FROM DriverDebtPayment p
            WHERE p.driverDebt.id IN :debtIds GROUP BY p.driverDebt.id
            """)
    List<AmountById> sumPaidByDebt(Collection<Integer> debtIds);

    @Query("""
            SELECT p.id AS id, p.paymentDate AS date FROM DriverDebtPayment p JOIN p.driverDebt d
            LEFT JOIN d.settlement origin
            WHERE p.paymentDate BETWEEN :startDate AND :endDate AND d.status <> CANCELLED
              AND (origin.id IS NULL OR (SELECT COUNT(DISTINCT s.weeklyClosing.owner.id)
                    FROM WeeklyClosingPochi s WHERE s.pochi.id = origin.pochi.id
                    AND origin.workDate BETWEEN s.weeklyClosing.weekStart AND s.weeklyClosing.weekEnd) > 1)
            ORDER BY p.paymentDate, p.id
            """)
    List<DatedId> findUnresolvedPayments(LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT e.id AS id, e.expenseDate AS date FROM Expense e
            WHERE e.expenseDate BETWEEN :startDate AND :endDate AND e.pochi IS NULL AND e.status = ACTIVE
            ORDER BY e.expenseDate, e.id
            """)
    List<DatedId> findGlobalExpenses(LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT c.id AS recordId, SUM(s.acceptedAmount) AS amount FROM WeeklyClosing c, Settlement s
            WHERE c.id IN :closingIds AND s.workDate BETWEEN c.weekStart AND c.weekEnd
              AND EXISTS (SELECT snap FROM WeeklyClosingPochi snap WHERE snap.weeklyClosing = c AND snap.pochi = s.pochi)
            GROUP BY c.id
            """)
    List<AmountById> sumAcceptedSettlements(Collection<Integer> closingIds);

    @Query("""
            SELECT c.id AS recordId, SUM(a.amount) AS amount FROM WeeklyClosing c, AdministrationCharge a
            WHERE c.id IN :closingIds AND a.chargeDate BETWEEN c.weekStart AND c.weekEnd
              AND EXISTS (SELECT snap FROM WeeklyClosingPochi snap WHERE snap.weeklyClosing = c AND snap.pochi = a.pochi)
            GROUP BY c.id
            """)
    List<AmountById> sumCharges(Collection<Integer> closingIds);

    @Query("""
            SELECT c.id AS recordId, SUM(e.amount) AS amount FROM WeeklyClosing c, Expense e
            WHERE c.id IN :closingIds AND e.expenseDate BETWEEN c.weekStart AND c.weekEnd AND e.status = ACTIVE
              AND EXISTS (SELECT snap FROM WeeklyClosingPochi snap WHERE snap.weeklyClosing = c AND snap.pochi = e.pochi)
            GROUP BY c.id
            """)
    List<AmountById> sumExpenses(Collection<Integer> closingIds);

    @Query("""
            SELECT c.id AS recordId, SUM(p.amount) AS amount
            FROM DriverDebtPayment p JOIN p.driverDebt d JOIN d.settlement origin, WeeklyClosing c
            WHERE c.id IN :closingIds AND p.paymentDate BETWEEN c.weekStart AND c.weekEnd AND d.status <> CANCELLED
            """ + ORIGIN_SCOPE + " GROUP BY c.id")
    List<AmountById> sumPayments(Collection<Integer> closingIds);

    interface AmountById {
        Integer getRecordId();
        BigDecimal getAmount();
    }

    interface DatedId {
        Integer getId();
        LocalDate getDate();
    }
}

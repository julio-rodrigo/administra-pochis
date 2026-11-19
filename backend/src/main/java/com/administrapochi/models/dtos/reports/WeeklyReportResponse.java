package com.administrapochi.models.dtos.reports;

import com.administrapochi.models.enums.DriverDebtStatus;
import com.administrapochi.models.enums.ExpenseStatus;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.enums.WeeklyClosingStatus;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReportResponse(
        WeekReportResponse week, WeeklyClosingReportResponse closing, List<PochiWeeklyReportResponse> pochis,
        FinancialSummaryResponse summary, List<ReportWarningResponse> warnings) {

    public record WeekReportResponse(
            Integer weekYear, Integer weekNumber, LocalDate startDate, LocalDate endDate) {}

    public record WeeklyClosingReportResponse(
            Integer weeklyClosingId, Integer ownerId, WeeklyClosingStatus status, String closingComment,
            Instant adminClosedAt, Instant ownerClosedAt, boolean preview) {}

    public record PochiWeeklyReportResponse(
            Integer pochiId, String pochiIdentifier, List<PochiDailyReportResponse> days,
            List<ExpenseReportResponse> expenses, List<DriverDebtReportResponse> debtsGenerated,
            List<DebtPaymentReportResponse> debtPayments, FinancialSummaryResponse summary) {}

    public record PochiDailyReportResponse(
            LocalDate date, DayOfWeek dayOfWeek, String dailyStatus, String statusComment,
            List<AssignmentReportResponse> assignments, AdministrationChargeReportResponse administrationCharge) {}

    public record AssignmentReportResponse(
            Integer assignmentId, Shift shift, DriverReportResponse driver, SettlementReportResponse settlement) {}

    public record DriverReportResponse(Integer driverId, String driverName) {}

    public record SettlementReportResponse(
            Integer settlementId, BigDecimal expectedAmount, BigDecimal acceptedAmount,
            BigDecimal difference, String adjustmentReason) {}

    public record AdministrationChargeReportResponse(
            Integer administrationChargeId, LocalDate chargeDate, BigDecimal amount, String note) {}

    public record ExpenseReportResponse(
            Integer expenseId, LocalDate expenseDate, Integer expenseTypeId, String expenseTypeCode,
            String expenseTypeName, BigDecimal amount, String description, Integer pochiId,
            String pochiIdentifier, ExpenseStatus status) {}

    public record DriverDebtReportResponse(
            Integer debtId, DriverReportResponse driver, BigDecimal originalAmount, BigDecimal currentBalance,
            DriverDebtStatus status, LocalDate debtDate, String description, DebtOriginReportResponse origin) {}

    public record DebtPaymentReportResponse(
            Integer paymentId, LocalDate paymentDate, BigDecimal amount, String notes, DriverReportResponse driver,
            Integer debtId, BigDecimal originalDebtAmount, BigDecimal balanceBeforePayment,
            BigDecimal balanceAfterPayment, DriverDebtStatus debtStatus, DebtOriginReportResponse origin) {}

    public record DebtOriginReportResponse(
            Integer settlementId, Integer pochiId, String pochiIdentifier, LocalDate originDate,
            Shift shift, List<Shift> originShifts) {}

    public record FinancialSummaryResponse(
            BigDecimal expectedSettlements, BigDecimal acceptedSettlements, BigDecimal settlementDifference,
            BigDecimal debtPaymentsReceived, BigDecimal grossReceived, BigDecimal administrationCharges,
            BigDecimal expenses, BigDecimal newDriverDebts, BigDecimal totalToOwner) {}

    public record ReportWarningResponse(
            String code, String message, Integer pochiId, LocalDate date, Shift shift, String sourceType,
            Integer sourceId) {}
}

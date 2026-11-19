package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.reports.WeeklyReportResponse.*;
import com.administrapochi.models.entities.*;
import com.administrapochi.models.enums.Shift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public interface ReportsMapper {

    @Mapping(target = "startDate", source = "weekStart")
    @Mapping(target = "endDate", source = "weekEnd")
    WeekReportResponse toWeek(WeeklyClosing closing);

    @Mapping(target = "weeklyClosingId", source = "closing.id")
    @Mapping(target = "ownerId", source = "closing.owner.id")
    WeeklyClosingReportResponse toClosing(WeeklyClosing closing, boolean preview);

    @Mapping(target = "assignmentId", source = "assignment.id")
    @Mapping(target = "driver", source = "assignment.driver")
    AssignmentReportResponse toAssignment(PochiAssignment assignment, Settlement settlement);

    @Mapping(target = "settlementId", source = "id")
    @Mapping(target = "difference", expression = "java(settlement.getAcceptedAmount().subtract(settlement.getExpectedAmount()))")
    SettlementReportResponse toSettlement(Settlement settlement);

    @Mapping(target = "administrationChargeId", source = "id")
    AdministrationChargeReportResponse toCharge(AdministrationCharge charge);

    @Mapping(target = "expenseId", source = "id")
    @Mapping(target = "expenseTypeId", source = "expenseType.id")
    @Mapping(target = "expenseTypeCode", source = "expenseType.code")
    @Mapping(target = "expenseTypeName", source = "expenseType.name")
    @Mapping(target = "pochiId", source = "pochi.id")
    @Mapping(target = "pochiIdentifier", source = "pochi.economicNumber")
    ExpenseReportResponse toExpense(Expense expense);

    @Mapping(target = "debtId", source = "debt.id")
    @Mapping(target = "originalAmount", source = "debt.amount")
    @Mapping(target = "origin", source = "debt.settlement")
    DriverDebtReportResponse toDebt(DriverDebt debt, BigDecimal currentBalance);

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "driver", source = "driverDebt.driver")
    @Mapping(target = "debtId", source = "driverDebt.id")
    @Mapping(target = "originalDebtAmount", source = "driverDebt.amount")
    @Mapping(target = "debtStatus", source = "driverDebt.status")
    @Mapping(target = "origin", source = "driverDebt.settlement")
    @Mapping(target = "balanceBeforePayment", ignore = true)
    @Mapping(target = "balanceAfterPayment", ignore = true)
    DebtPaymentReportResponse toPayment(DriverDebtPayment payment);

    default DriverReportResponse toDriver(Driver driver) {
        if (driver == null) {
            return null;
        }
        String name = Stream.of(driver.getFirstName(), driver.getFirstLastName(), driver.getSecondLastName())
                .filter(Objects::nonNull).filter(part -> !part.isBlank()).collect(Collectors.joining(" "));
        return new DriverReportResponse(driver.getId(), name);
    }

    default DebtOriginReportResponse toOrigin(Settlement settlement) {
        if (settlement == null) {
            return null;
        }
        List<Shift> shifts = settlement.getPochiAssignments().stream()
                .map(PochiAssignment::getShift).filter(Objects::nonNull).distinct().sorted().toList();
        return new DebtOriginReportResponse(settlement.getId(), settlement.getPochi().getId(),
                settlement.getPochi().getEconomicNumber(), settlement.getWorkDate(),
                shifts.size() == 1 ? shifts.getFirst() : null, shifts);
    }
}

package com.administrapochi.models.dtos.reports;

import com.administrapochi.models.enums.WeeklyClosingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record WeeklyReportHistoryResponse(
        Integer weeklyClosingId, Integer ownerId, Integer weekYear, Integer weekNumber, LocalDate startDate,
        LocalDate endDate, WeeklyClosingStatus status, String closingComment, Instant adminClosedAt,
        Instant ownerClosedAt, BigDecimal acceptedSettlements, BigDecimal debtPaymentsReceived,
        BigDecimal administrationCharges, BigDecimal expenses, BigDecimal totalToOwner) {}

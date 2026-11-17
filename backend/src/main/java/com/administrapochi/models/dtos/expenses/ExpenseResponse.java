package com.administrapochi.models.dtos.expenses;

import com.administrapochi.models.enums.ExpenseStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ExpenseResponse {

    private Integer id;
    private Integer expenseTypeId;
    private String expenseTypeCode;
    private String expenseTypeName;
    private Integer pochiId;
    private String pochiIdentifier;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private Integer paidByUserId;
    private String paidByUserName;
    private Integer registeredByUserId;
    private String registeredByUserName;
    private String description;
    private ExpenseStatus status;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private Integer cancelledByUserId;
    private String cancelledByUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

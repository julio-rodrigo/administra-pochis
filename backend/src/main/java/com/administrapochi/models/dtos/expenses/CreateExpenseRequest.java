package com.administrapochi.models.dtos.expenses;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateExpenseRequest {

    @NotNull
    @Positive
    private Integer expenseTypeId;

    @Positive
    private Integer pochiId;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotNull
    private LocalDate expenseDate;

    private String description;
}

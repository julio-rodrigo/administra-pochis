package com.administrapochi.models.dtos.settlements;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SettlementCreateRequest {

    @NotEmpty
    @Size(max = 2)
    private List<@NotNull @Positive Integer> pochiAssignmentIds;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal acceptedAmount;

    @Size(max = 500)
    private String adjustmentReason;

    @Size(max = 500)
    private String notes;
}

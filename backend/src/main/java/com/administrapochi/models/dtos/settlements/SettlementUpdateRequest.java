package com.administrapochi.models.dtos.settlements;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SettlementUpdateRequest {

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal acceptedAmount;

    @Size(max = 500)
    private String adjustmentReason;

    @Size(max = 500)
    private String notes;
}

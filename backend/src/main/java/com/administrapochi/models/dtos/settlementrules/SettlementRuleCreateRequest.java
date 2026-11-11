package com.administrapochi.models.dtos.settlementrules;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SettlementRuleCreateRequest {

    private Integer pochiId;

    @NotNull
    @Min(1)
    @Max(2)
    private Integer shiftCount;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal amount;

    @NotNull
    private LocalDate validFrom;

    private LocalDate validTo;

    @AssertTrue(message = "validTo must be on or after validFrom")
    public boolean isValidDateRange() {
        return validFrom == null || validTo == null || !validTo.isBefore(validFrom);
    }
}

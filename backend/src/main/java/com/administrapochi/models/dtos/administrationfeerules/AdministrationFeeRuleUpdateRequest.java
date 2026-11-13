package com.administrapochi.models.dtos.administrationfeerules;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AdministrationFeeRuleUpdateRequest {

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal oneShiftAmount;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal twoShiftsAmount;

    @NotNull
    private LocalDate validFrom;

    private LocalDate validTo;

    @NotNull
    private Boolean active;
}

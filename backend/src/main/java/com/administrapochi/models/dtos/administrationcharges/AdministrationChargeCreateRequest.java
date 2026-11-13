package com.administrapochi.models.dtos.administrationcharges;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AdministrationChargeCreateRequest {

    @NotNull
    @Positive
    private Integer pochiId;

    @NotNull
    private LocalDate chargeDate;

    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal amount;

    @Size(max = 500)
    private String note;
}

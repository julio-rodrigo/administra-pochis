package com.administrapochi.models.dtos.driverdebts;

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
public class DriverDebtPaymentCreateRequest {

    @NotNull
    @Positive
    @Digits(integer = 8, fraction = 2)
    private BigDecimal amount;

    @NotNull
    private LocalDate paymentDate;

    @Size(max = 500)
    private String notes;
}

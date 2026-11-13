package com.administrapochi.models.dtos.driverdebts;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class DriverDebtCreateRequest {

    @NotNull
    @Positive
    private Integer driverId;

    @Positive
    private Integer settlementId;

    @NotNull
    @Positive
    @Digits(integer = 8, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotNull
    private LocalDate debtDate;
}

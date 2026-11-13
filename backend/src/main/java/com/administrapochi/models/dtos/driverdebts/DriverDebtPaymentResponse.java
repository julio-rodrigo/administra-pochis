package com.administrapochi.models.dtos.driverdebts;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DriverDebtPaymentResponse {

    private Integer id;
    private Integer driverDebtId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String notes;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}

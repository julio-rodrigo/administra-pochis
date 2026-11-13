package com.administrapochi.models.dtos.driverdebts;

import com.administrapochi.models.enums.DriverDebtStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class DriverDebtResponse {

    private Integer id;
    private Integer driverId;
    private String driverName;
    private Integer settlementId;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private DriverDebtStatus status;
    private String description;
    private LocalDate debtDate;
    private Integer createdById;
    private String createdByName;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private Integer cancelledById;
    private String cancelledByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

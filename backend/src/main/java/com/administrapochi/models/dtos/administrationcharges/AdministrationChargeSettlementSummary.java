package com.administrapochi.models.dtos.administrationcharges;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdministrationChargeSettlementSummary {

    private Integer settlementId;
    private List<Integer> pochiAssignmentIds;
    private String shifts;
    private BigDecimal expectedAmount;
    private BigDecimal acceptedAmount;
    private LocalDateTime settledAt;
}

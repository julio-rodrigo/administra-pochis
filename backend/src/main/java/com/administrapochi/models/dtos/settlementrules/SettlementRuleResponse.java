package com.administrapochi.models.dtos.settlementrules;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SettlementRuleResponse {

    private Integer id;
    private Integer pochiId;
    private String pochiEconomicNumber;
    private Integer shiftCount;
    private BigDecimal amount;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.administrapochi.models.dtos.administrationfeerules;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdministrationFeeRuleResponse {

    private Integer id;
    private Integer pochiId;
    private String pochiIdentifier;
    private BigDecimal oneShiftAmount;
    private BigDecimal twoShiftsAmount;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
    private Boolean global;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

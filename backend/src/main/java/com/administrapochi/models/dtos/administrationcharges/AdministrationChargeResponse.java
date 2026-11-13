package com.administrapochi.models.dtos.administrationcharges;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdministrationChargeResponse {

    private Integer id;
    private Integer pochiId;
    private String pochiIdentifier;
    private LocalDate chargeDate;
    private Integer administrationFeeRuleId;
    private Boolean globalRule;
    private Short shiftCount;
    private BigDecimal suggestedAmount;
    private BigDecimal amount;
    private Boolean adjusted;
    private String note;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<AdministrationChargeSettlementSummary> settlements;
}

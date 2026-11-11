package com.administrapochi.models.dtos.settlementrules;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SettlementRuleValidToUpdateRequest {

    @NotNull
    private LocalDate validTo;
}

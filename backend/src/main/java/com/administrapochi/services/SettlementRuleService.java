package com.administrapochi.services;

import com.administrapochi.models.dtos.settlementrules.SettlementRuleCreateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleResponse;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleUpdateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleValidToUpdateRequest;
import com.administrapochi.models.entities.SettlementRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SettlementRuleService {

    SettlementRuleResponse create(SettlementRuleCreateRequest request);

    SettlementRuleResponse findById(Integer id);

    Page<SettlementRuleResponse> findAll(
            Integer pochiId, Integer shiftCount, LocalDate validOn, Pageable pageable);

    SettlementRuleResponse update(Integer id, SettlementRuleUpdateRequest request);

    SettlementRuleResponse updateValidTo(Integer id, SettlementRuleValidToUpdateRequest request);

    SettlementRule resolveApplicableRule(Integer pochiId, Integer shiftCount, LocalDate date);
}

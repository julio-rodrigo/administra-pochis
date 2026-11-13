package com.administrapochi.services;

import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleCreateRequest;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleResponse;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleUpdateRequest;
import com.administrapochi.models.entities.AdministrationFeeRule;

import java.time.LocalDate;
import java.util.List;

public interface AdministrationFeeRuleService {

    AdministrationFeeRuleResponse create(AdministrationFeeRuleCreateRequest request);

    /**
     * Updates a rule without changing its Pochi scope. Existing Administration Charges retain
     * their stored suggested and actual amounts regardless of subsequent rule changes.
     */
    AdministrationFeeRuleResponse update(Integer id, AdministrationFeeRuleUpdateRequest request);

    AdministrationFeeRuleResponse findById(Integer id);

    List<AdministrationFeeRuleResponse> findAll(
            Integer pochiId, Boolean global, Boolean active, LocalDate validOn);

    AdministrationFeeRuleResponse findApplicableRule(Integer pochiId, LocalDate date);

    AdministrationFeeRule resolveApplicableRuleEntity(Integer pochiId, LocalDate date);

    AdministrationFeeRuleResponse deactivate(Integer id);
}

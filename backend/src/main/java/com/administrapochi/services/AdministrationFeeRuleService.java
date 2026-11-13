package com.administrapochi.services;

import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleCreateRequest;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleResponse;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface AdministrationFeeRuleService {

    AdministrationFeeRuleResponse create(AdministrationFeeRuleCreateRequest request);

    /**
     * Updates a rule without changing its Pochi scope. Historical usage cannot be checked
     * until Administration Charges are implemented; changes to future fees should use a new rule.
     */
    AdministrationFeeRuleResponse update(Integer id, AdministrationFeeRuleUpdateRequest request);

    AdministrationFeeRuleResponse findById(Integer id);

    List<AdministrationFeeRuleResponse> findAll(
            Integer pochiId, Boolean global, Boolean active, LocalDate validOn);

    AdministrationFeeRuleResponse findApplicableRule(Integer pochiId, LocalDate date);

    AdministrationFeeRuleResponse deactivate(Integer id);
}

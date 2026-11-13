package com.administrapochi.services;

import com.administrapochi.exceptions.AdministrationFeeRuleNotFoundException;
import com.administrapochi.exceptions.AdministrationFeeRuleOverlapException;
import com.administrapochi.exceptions.InvalidAdministrationFeeRuleException;
import com.administrapochi.exceptions.NoApplicableAdministrationFeeRuleException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleCreateRequest;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleResponse;
import com.administrapochi.models.dtos.administrationfeerules.AdministrationFeeRuleUpdateRequest;
import com.administrapochi.models.entities.AdministrationFeeRule;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.mappers.AdministrationFeeRuleMapper;
import com.administrapochi.repositories.AdministrationFeeRuleRepository;
import com.administrapochi.repositories.PochiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdministrationFeeRuleServiceImpl implements AdministrationFeeRuleService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AdministrationFeeRuleRepository ruleRepository;
    private final PochiRepository pochiRepository;
    private final AdministrationFeeRuleMapper ruleMapper;

    @Override
    @Transactional
    public AdministrationFeeRuleResponse create(AdministrationFeeRuleCreateRequest request) {
        validateAmounts(request.getOneShiftAmount(), request.getTwoShiftsAmount());
        validateDateRange(request.getValidFrom(), request.getValidTo());
        Pochi pochi = findOptionalPochi(request.getPochiId());
        validateNoOverlap(request.getPochiId(), request.getValidFrom(), request.getValidTo(), null);

        AdministrationFeeRule rule = ruleMapper.toEntity(request);
        rule.setPochi(pochi);
        if (rule.getActive() == null) {
            rule.setActive(true);
        }
        return ruleMapper.toResponse(ruleRepository.save(rule));
    }

    @Override
    @Transactional
    public AdministrationFeeRuleResponse update(
            Integer id, AdministrationFeeRuleUpdateRequest request) {
        AdministrationFeeRule rule = findRule(id);
        validateAmounts(request.getOneShiftAmount(), request.getTwoShiftsAmount());
        validateDateRange(request.getValidFrom(), request.getValidTo());

        Integer pochiId = rule.getPochi() == null ? null : rule.getPochi().getId();
        validateNoOverlap(pochiId, request.getValidFrom(), request.getValidTo(), id);

        ruleMapper.updateEntity(request, rule);
        return ruleMapper.toResponse(ruleRepository.save(rule));
    }

    @Override
    public AdministrationFeeRuleResponse findById(Integer id) {
        return ruleMapper.toResponse(findRule(id));
    }

    @Override
    public List<AdministrationFeeRuleResponse> findAll(
            Integer pochiId, Boolean global, Boolean active, LocalDate validOn) {
        if (Boolean.TRUE.equals(global) && pochiId != null) {
            throw new InvalidAdministrationFeeRuleException(
                    "pochiId cannot be combined with global=true.");
        }
        if (pochiId != null && pochiId <= 0) {
            throw new InvalidAdministrationFeeRuleException("pochiId must be a positive integer.");
        }

        Specification<AdministrationFeeRule> specification =
                (root, query, builder) -> builder.conjunction();
        if (pochiId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("pochi").get("id"), pochiId));
        }
        if (Boolean.TRUE.equals(global)) {
            specification = specification.and((root, query, builder) ->
                    builder.isNull(root.get("pochi")));
        } else if (Boolean.FALSE.equals(global)) {
            specification = specification.and((root, query, builder) ->
                    builder.isNotNull(root.get("pochi")));
        }
        if (active != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("active"), active));
        }
        if (validOn != null) {
            specification = specification.and((root, query, builder) -> builder.and(
                    builder.lessThanOrEqualTo(root.get("validFrom"), validOn),
                    builder.or(
                            builder.isNull(root.get("validTo")),
                            builder.greaterThanOrEqualTo(root.get("validTo"), validOn))));
        }

        Sort sort = Sort.by(Sort.Order.desc("validFrom"), Sort.Order.asc("id"));
        return ruleMapper.toResponseList(ruleRepository.findAll(specification, sort));
    }

    @Override
    public AdministrationFeeRuleResponse findApplicableRule(Integer pochiId, LocalDate date) {
        return ruleMapper.toResponse(resolveApplicableRuleEntity(pochiId, date));
    }

    @Override
    public AdministrationFeeRule resolveApplicableRuleEntity(Integer pochiId, LocalDate date) {
        if (pochiId == null || pochiId <= 0) {
            throw new InvalidAdministrationFeeRuleException("pochiId must be a positive integer.");
        }
        if (date == null) {
            throw new InvalidAdministrationFeeRuleException("date is required.");
        }
        findPochi(pochiId);

        List<AdministrationFeeRule> specificRules =
                ruleRepository.findApplicableForPochi(pochiId, date);
        if (specificRules.size() > 1) {
            throw new AdministrationFeeRuleOverlapException(pochiId);
        }
        if (!specificRules.isEmpty()) {
            return specificRules.getFirst();
        }

        List<AdministrationFeeRule> globalRules = ruleRepository.findApplicableGlobalRule(date);
        if (globalRules.size() > 1) {
            throw new AdministrationFeeRuleOverlapException(null);
        }
        if (!globalRules.isEmpty()) {
            return globalRules.getFirst();
        }
        throw new NoApplicableAdministrationFeeRuleException(pochiId, date);
    }

    @Override
    @Transactional
    public AdministrationFeeRuleResponse deactivate(Integer id) {
        AdministrationFeeRule rule = findRule(id);
        if (Boolean.TRUE.equals(rule.getActive())) {
            rule.setActive(false);
            rule = ruleRepository.save(rule);
        }
        return ruleMapper.toResponse(rule);
    }

    private AdministrationFeeRule findRule(Integer id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new AdministrationFeeRuleNotFoundException(id));
    }

    private Pochi findPochi(Integer id) {
        return pochiRepository.findById(id)
                .orElseThrow(() -> new PochiNotFoundException(id));
    }

    private Pochi findOptionalPochi(Integer id) {
        if (id == null) {
            return null;
        }
        if (id <= 0) {
            throw new InvalidAdministrationFeeRuleException("pochiId must be a positive integer.");
        }
        return findPochi(id);
    }

    private void validateAmounts(BigDecimal oneShiftAmount, BigDecimal twoShiftsAmount) {
        if (!isValidAmount(oneShiftAmount) || !isValidAmount(twoShiftsAmount)) {
            throw new InvalidAdministrationFeeRuleException(
                    "Administration fee amounts must be greater than or equal to zero.");
        }
        if (twoShiftsAmount.compareTo(oneShiftAmount) < 0) {
            throw new InvalidAdministrationFeeRuleException(
                    "twoShiftAmount must be greater than or equal to oneShiftAmount.");
        }
    }

    private boolean isValidAmount(BigDecimal amount) {
        return amount != null
                && amount.compareTo(ZERO) >= 0
                && amount.scale() <= 2
                && amount.precision() - amount.scale() <= 8;
    }

    private void validateDateRange(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null || (validTo != null && validTo.isBefore(validFrom))) {
            throw new InvalidAdministrationFeeRuleException(
                    "validTo must be greater than or equal to validFrom.");
        }
    }

    private void validateNoOverlap(
            Integer pochiId, LocalDate validFrom, LocalDate validTo, Integer excludedId) {
        boolean overlaps = pochiId == null
                ? ruleRepository.existsOverlappingGlobalRule(validFrom, validTo, excludedId)
                : ruleRepository.existsOverlappingPochiRule(pochiId, validFrom, validTo, excludedId);
        if (overlaps) {
            throw new AdministrationFeeRuleOverlapException(pochiId);
        }
    }
}

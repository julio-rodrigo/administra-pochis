package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidSettlementRuleDateRangeException;
import com.administrapochi.exceptions.NoApplicableSettlementRuleException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.exceptions.SettlementRuleNotFoundException;
import com.administrapochi.exceptions.SettlementRuleOverlapException;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleCreateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleResponse;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleUpdateRequest;
import com.administrapochi.models.dtos.settlementrules.SettlementRuleValidToUpdateRequest;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.SettlementRule;
import com.administrapochi.models.mappers.SettlementRuleMapper;
import com.administrapochi.repositories.PochiRepository;
import com.administrapochi.repositories.SettlementRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementRuleServiceImpl implements SettlementRuleService {

    private final SettlementRuleRepository settlementRuleRepository;
    private final PochiRepository pochiRepository;
    private final SettlementRuleMapper settlementRuleMapper;

    @Override
    @Transactional
    public SettlementRuleResponse create(SettlementRuleCreateRequest request) {
        validateDateRange(request.getValidFrom(), request.getValidTo());
        Pochi pochi = findOptionalPochi(request.getPochiId());
        validateNoOverlap(request.getPochiId(), request.getShiftCount(),
                request.getValidFrom(), request.getValidTo(), null);

        SettlementRule rule = settlementRuleMapper.toEntity(request);
        rule.setPochi(pochi);
        return settlementRuleMapper.toResponse(settlementRuleRepository.save(rule));
    }

    @Override
    public SettlementRuleResponse findById(Integer id) {
        return settlementRuleMapper.toResponse(findRule(id));
    }

    @Override
    public Page<SettlementRuleResponse> findAll(
            Integer pochiId, Integer shiftCount, LocalDate validOn, Pageable pageable) {
        Specification<SettlementRule> specification = (root, query, builder) -> builder.conjunction();
        if (pochiId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("pochi").get("id"), pochiId));
        }
        if (shiftCount != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("shiftCount"), shiftCount.shortValue()));
        }
        if (validOn != null) {
            specification = specification.and((root, query, builder) -> builder.and(
                    builder.lessThanOrEqualTo(root.get("validFrom"), validOn),
                    builder.or(
                            builder.isNull(root.get("validTo")),
                            builder.greaterThanOrEqualTo(root.get("validTo"), validOn))));
        }
        return settlementRuleRepository.findAll(specification, pageable).map(settlementRuleMapper::toResponse);
    }

    @Override
    @Transactional
    public SettlementRuleResponse update(Integer id, SettlementRuleUpdateRequest request) {
        SettlementRule rule = findRule(id);
        validateDateRange(request.getValidFrom(), request.getValidTo());
        Pochi pochi = findOptionalPochi(request.getPochiId());
        validateNoOverlap(request.getPochiId(), request.getShiftCount(),
                request.getValidFrom(), request.getValidTo(), id);

        settlementRuleMapper.updateEntity(request, rule);
        rule.setPochi(pochi);
        return settlementRuleMapper.toResponse(settlementRuleRepository.save(rule));
    }

    @Override
    @Transactional
    public SettlementRuleResponse updateValidTo(Integer id, SettlementRuleValidToUpdateRequest request) {
        SettlementRule rule = findRule(id);
        LocalDate validTo = request.getValidTo();
        if (validTo == null || validTo.isBefore(rule.getValidFrom())) {
            throw new InvalidSettlementRuleDateRangeException();
        }

        Integer pochiId = rule.getPochi() == null ? null : rule.getPochi().getId();
        validateNoOverlap(pochiId, rule.getShiftCount().intValue(), rule.getValidFrom(), validTo, id);

        rule.setValidTo(validTo);
        return settlementRuleMapper.toResponse(settlementRuleRepository.save(rule));
    }

    @Override
    public SettlementRule resolveApplicableRule(Integer pochiId, Integer shiftCount, LocalDate date) {
        if (pochiId != null) {
            findPochi(pochiId);
            List<SettlementRule> specificRules =
                    settlementRuleRepository.findApplicableForPochi(pochiId, toShiftCount(shiftCount), date);
            if (!specificRules.isEmpty()) {
                return specificRules.getFirst();
            }
        }

        List<SettlementRule> generalRules =
                settlementRuleRepository.findApplicableGeneral(toShiftCount(shiftCount), date);
        if (!generalRules.isEmpty()) {
            return generalRules.getFirst();
        }
        throw new NoApplicableSettlementRuleException(pochiId, shiftCount, date);
    }

    private SettlementRule findRule(Integer id) {
        return settlementRuleRepository.findById(id)
                .orElseThrow(() -> new SettlementRuleNotFoundException(id));
    }

    private Pochi findPochi(Integer id) {
        return pochiRepository.findById(id)
                .orElseThrow(() -> new PochiNotFoundException(id));
    }

    private Pochi findOptionalPochi(Integer id) {
        return id == null ? null : findPochi(id);
    }

    private void validateDateRange(LocalDate validFrom, LocalDate validTo) {
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new InvalidSettlementRuleDateRangeException();
        }
    }

    private void validateNoOverlap(
            Integer pochiId, Integer shiftCount, LocalDate validFrom, LocalDate validTo, Integer excludedId) {
        Short shiftCountValue = toShiftCount(shiftCount);
        boolean overlaps = pochiId == null
                ? settlementRuleRepository.existsOverlappingGeneralRule(
                        shiftCountValue, validFrom, validTo, excludedId)
                : settlementRuleRepository.existsOverlappingPochiRule(
                        pochiId, shiftCountValue, validFrom, validTo, excludedId);
        if (overlaps) {
            throw new SettlementRuleOverlapException();
        }
    }

    private Short toShiftCount(Integer shiftCount) {
        return shiftCount == null ? null : shiftCount.shortValue();
    }
}

package com.administrapochi.services;

import com.administrapochi.exceptions.AdministrationChargeConflictException;
import com.administrapochi.exceptions.AdministrationChargeNotFoundException;
import com.administrapochi.exceptions.InvalidAdministrationChargeException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeCreateRequest;
import com.administrapochi.models.dtos.administrationcharges.AdministrationChargeResponse;
import com.administrapochi.models.entities.AdministrationCharge;
import com.administrapochi.models.entities.AdministrationChargeSettlement;
import com.administrapochi.models.entities.AdministrationChargeSettlementId;
import com.administrapochi.models.entities.AdministrationFeeRule;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.entities.Settlement;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.Shift;
import com.administrapochi.models.mappers.AdministrationChargeMapper;
import com.administrapochi.repositories.AdministrationChargeRepository;
import com.administrapochi.repositories.AdministrationChargeSettlementRepository;
import com.administrapochi.repositories.PochiAssignmentRepository;
import com.administrapochi.repositories.PochiRepository;
import com.administrapochi.repositories.SettlementRepository;
import com.administrapochi.repositories.UserRepository;
import com.administrapochi.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdministrationChargeServiceImpl implements AdministrationChargeService {

    private static final Integer TEMPORARY_USER_ID = 2;

    private final AdministrationChargeRepository chargeRepository;
    private final AdministrationChargeSettlementRepository chargeSettlementRepository;
    private final PochiRepository pochiRepository;
    private final PochiAssignmentRepository assignmentRepository;
    private final SettlementRepository settlementRepository;
    private final AdministrationFeeRuleService feeRuleService;
    private final UserRepository userRepository;
    private final AdministrationChargeMapper chargeMapper;

    @Override
    @Transactional
    public AdministrationChargeResponse create(AdministrationChargeCreateRequest request) {
        validateRequest(request);
        if (chargeRepository.existsByPochiIdAndChargeDate(request.getPochiId(), request.getChargeDate())) {
            throw dailyChargeConflict(request.getPochiId(), request.getChargeDate());
        }

        Pochi pochi = pochiRepository.findById(request.getPochiId())
                .orElseThrow(() -> new PochiNotFoundException(request.getPochiId()));
        List<PochiAssignment> assignments =
                assignmentRepository.findForAdministrationCharge(pochi.getId(), request.getChargeDate());
        validateAssignments(assignments, pochi.getId(), request.getChargeDate());

        List<Settlement> settlements = resolveSettlements(assignments);
        Set<Integer> linkedSettlementIds = chargeSettlementRepository.findLinkedSettlementIds(
                settlements.stream().map(Settlement::getId).toList());
        for (Settlement settlement : settlements) {
            if (linkedSettlementIds.contains(settlement.getId())) {
                throw new AdministrationChargeConflictException(
                        "Settlement " + settlement.getId() + " already belongs to an Administration Charge.");
            }
        }

        AdministrationFeeRule rule =
                feeRuleService.resolveApplicableRuleEntity(pochi.getId(), request.getChargeDate());
        Short shiftCount = (short) assignments.size();
        BigDecimal suggestedAmount = shiftCount == 1
                ? rule.getOneShiftAmount()
                : rule.getTwoShiftsAmount();
        if (suggestedAmount == null || suggestedAmount.signum() <= 0) {
            throw new InvalidAdministrationChargeException(
                    "The applicable Administration Fee Rule must provide a positive amount.");
        }

        BigDecimal amount = request.getAmount() == null ? suggestedAmount : request.getAmount();
        validateAmount(amount);
        String note = request.getNote() == null ? null : request.getNote().trim();
        if (amount.compareTo(suggestedAmount) != 0 && (note == null || note.isBlank())) {
            throw new InvalidAdministrationChargeException(
                    "note is required when amount differs from suggestedAmount.");
        }

        User creator = userRepository.findById(TEMPORARY_USER_ID)
                .orElseThrow(() -> new UserNotFoundException(TEMPORARY_USER_ID));
        AdministrationCharge charge = AdministrationCharge.builder()
                .pochi(pochi)
                .chargeDate(request.getChargeDate())
                .administrationFeeRule(rule)
                .shiftCount(shiftCount)
                .suggestedAmount(suggestedAmount)
                .amount(amount)
                .note(note)
                .createdBy(creator)
                .chargeSettlements(new ArrayList<>())
                .build();

        List<AdministrationChargeSettlement> links;
        try {
            chargeRepository.saveAndFlush(charge);
            links = settlements.stream()
                    .map(settlement -> AdministrationChargeSettlement.builder()
                            .id(new AdministrationChargeSettlementId(charge.getId(), settlement.getId()))
                            .administrationCharge(charge)
                            .settlement(settlement)
                            .build())
                    .toList();
            chargeSettlementRepository.saveAllAndFlush(links);
        } catch (DataIntegrityViolationException exception) {
            throw new AdministrationChargeConflictException(
                    "The Administration Charge conflicts with an existing daily charge or Settlement association.");
        }
        return chargeMapper.toResponse(charge, links);
    }

    @Override
    public AdministrationChargeResponse findById(Integer id) {
        AdministrationCharge charge = chargeRepository.findWithDetailsById(id)
                .orElseThrow(() -> new AdministrationChargeNotFoundException(id));
        List<AdministrationChargeSettlement> links =
                loadSettlementLinks(List.of(charge)).getOrDefault(charge.getId(), List.of());
        return chargeMapper.toResponse(charge, links);
    }

    @Override
    public Page<AdministrationChargeResponse> findAll(
            Integer pochiId,
            LocalDate chargeDate,
            LocalDate chargeDateFrom,
            LocalDate chargeDateTo,
            Integer createdById,
            Boolean adjusted,
            Pageable pageable) {
        if ((pochiId != null && pochiId <= 0)
                || (createdById != null && createdById <= 0)
                || (chargeDateFrom != null && chargeDateTo != null && chargeDateTo.isBefore(chargeDateFrom))
                || (chargeDate != null && (chargeDateFrom != null || chargeDateTo != null))) {
            throw new InvalidAdministrationChargeException(
                    "Administration Charge filters contain invalid or contradictory values.");
        }

        Specification<AdministrationCharge> specification =
                (root, query, builder) -> builder.conjunction();
        if (pochiId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("pochi").get("id"), pochiId));
        }
        if (chargeDate != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("chargeDate"), chargeDate));
        }
        if (chargeDateFrom != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("chargeDate"), chargeDateFrom));
        }
        if (chargeDateTo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("chargeDate"), chargeDateTo));
        }
        if (createdById != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("createdBy").get("id"), createdById));
        }
        if (adjusted != null) {
            specification = specification.and((root, query, builder) -> adjusted
                    ? builder.notEqual(root.get("amount"), root.get("suggestedAmount"))
                    : builder.equal(root.get("amount"), root.get("suggestedAmount")));
        }

        Page<AdministrationCharge> charges = chargeRepository.findAll(specification, pageable);
        Map<Integer, List<AdministrationChargeSettlement>> linksByChargeId =
                loadSettlementLinks(charges.getContent());
        return charges.map(charge -> chargeMapper.toResponse(
                charge, linksByChargeId.getOrDefault(charge.getId(), List.of())));
    }

    private List<Settlement> resolveSettlements(List<PochiAssignment> assignments) {
        List<Integer> assignmentIds = assignments.stream().map(PochiAssignment::getId).toList();
        List<Settlement> foundSettlements = settlementRepository.findByPochiAssignmentIds(assignmentIds);
        Map<Integer, Settlement> byAssignmentId = new HashMap<>();
        for (Settlement settlement : foundSettlements) {
            for (PochiAssignment assignment : settlement.getPochiAssignments()) {
                if (assignmentIds.contains(assignment.getId())) {
                    byAssignmentId.put(assignment.getId(), settlement);
                }
            }
        }

        for (PochiAssignment assignment : assignments) {
            if (!byAssignmentId.containsKey(assignment.getId())) {
                throw new InvalidAdministrationChargeException(
                        assignment.getShift().name() + " Pochi Assignment has no Settlement.");
            }
        }
        return assignments.stream()
                .map(assignment -> byAssignmentId.get(assignment.getId()))
                .distinct()
                .toList();
    }

    private void validateAssignments(List<PochiAssignment> assignments, Integer pochiId, LocalDate chargeDate) {
        if (assignments.isEmpty()) {
            throw new InvalidAdministrationChargeException(
                    "No Pochi Assignment exists for Pochi " + pochiId + " on " + chargeDate + ".");
        }
        if (assignments.size() > 2) {
            throw new InvalidAdministrationChargeException(
                    "A daily Administration Charge can include no more than two shifts.");
        }
        if (assignments.stream().anyMatch(assignment -> assignment.getShift() == null)
                || assignments.stream().map(PochiAssignment::getShift).distinct().count() != assignments.size()) {
            throw new InvalidAdministrationChargeException(
                    "Pochi Assignments must have distinct MORNING and AFTERNOON shifts.");
        }
        if (assignments.size() == 2
                && !assignments.stream().map(PochiAssignment::getShift).collect(Collectors.toSet())
                .equals(Set.of(Shift.MORNING, Shift.AFTERNOON))) {
            throw new InvalidAdministrationChargeException(
                    "A two-shift Administration Charge requires one MORNING and one AFTERNOON Assignment.");
        }
    }

    private void validateRequest(AdministrationChargeCreateRequest request) {
        if (request == null || request.getPochiId() == null || request.getPochiId() <= 0
                || request.getChargeDate() == null) {
            throw new InvalidAdministrationChargeException("pochiId and chargeDate are required.");
        }
        if (request.getNote() != null && request.getNote().length() > 500) {
            throw new InvalidAdministrationChargeException("note cannot exceed 500 characters.");
        }
        if (request.getAmount() != null) {
            validateAmount(request.getAmount());
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0
                || amount.scale() > 2 || amount.precision() - amount.scale() > 8) {
            throw new InvalidAdministrationChargeException(
                    "amount must be greater than zero and have at most 8 integer and 2 decimal digits.");
        }
    }

    private Map<Integer, List<AdministrationChargeSettlement>> loadSettlementLinks(
            List<AdministrationCharge> charges) {
        if (charges.isEmpty()) {
            return Map.of();
        }
        List<Integer> chargeIds = charges.stream().map(AdministrationCharge::getId).toList();
        return chargeSettlementRepository.findAllByAdministrationChargeIdIn(chargeIds).stream()
                .collect(Collectors.groupingBy(link -> link.getId().getAdministrationChargeId()));
    }

    private AdministrationChargeConflictException dailyChargeConflict(Integer pochiId, LocalDate date) {
        return new AdministrationChargeConflictException(
                "An Administration Charge already exists for Pochi " + pochiId + " on " + date + ".");
    }
}

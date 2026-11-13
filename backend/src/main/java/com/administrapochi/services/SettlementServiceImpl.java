package com.administrapochi.services;

import com.administrapochi.exceptions.InvalidSettlementException;
import com.administrapochi.exceptions.InvalidSettlementFilterException;
import com.administrapochi.exceptions.AdministrationChargeConflictException;
import com.administrapochi.exceptions.PochiAssignmentAlreadySettledException;
import com.administrapochi.exceptions.PochiAssignmentNotFoundException;
import com.administrapochi.exceptions.SettlementNotFoundException;
import com.administrapochi.models.dtos.settlements.SettlementCreateRequest;
import com.administrapochi.models.dtos.settlements.SettlementResponse;
import com.administrapochi.models.dtos.settlements.SettlementUpdateRequest;
import com.administrapochi.models.entities.PochiAssignment;
import com.administrapochi.models.entities.Settlement;
import com.administrapochi.models.entities.SettlementRule;
import com.administrapochi.models.mappers.SettlementMapper;
import com.administrapochi.repositories.PochiAssignmentRepository;
import com.administrapochi.repositories.AdministrationChargeSettlementRepository;
import com.administrapochi.repositories.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private static final String ASSIGNMENT_UNIQUE_CONSTRAINT =
            "uq_settlement_assignments_pochi_assignment";

    private final SettlementRepository settlementRepository;
    private final AdministrationChargeSettlementRepository chargeSettlementRepository;
    private final PochiAssignmentRepository assignmentRepository;
    private final SettlementRuleService settlementRuleService;
    private final SettlementMapper settlementMapper;

    @Override
    @Transactional
    public SettlementResponse create(SettlementCreateRequest request) {
        List<Integer> requestedIds = request.getPochiAssignmentIds();
        validateRequestAmounts(request.getAcceptedAmount());
        validateUniqueAssignmentIds(requestedIds);

        List<PochiAssignment> assignments = resolveAssignments(requestedIds);
        validateAssignmentsAvailable(requestedIds);
        validateCompatibleAssignments(assignments);

        PochiAssignment firstAssignment = assignments.getFirst();
        SettlementRule rule = settlementRuleService.resolveApplicableRule(
                firstAssignment.getPochi().getId(), assignments.size(), firstAssignment.getAssignmentDate());
        if (rule.getAmount() == null || rule.getAmount().signum() < 0) {
            throw new InvalidSettlementException("The applicable Settlement Rule has an invalid amount.");
        }
        validateAdjustment(request.getAcceptedAmount(), rule.getAmount(), request.getAdjustmentReason());

        Settlement settlement = Settlement.builder()
                .driver(firstAssignment.getDriver())
                .pochi(firstAssignment.getPochi())
                .workDate(firstAssignment.getAssignmentDate())
                .expectedAmount(rule.getAmount())
                .acceptedAmount(request.getAcceptedAmount())
                .adjustmentReason(request.getAdjustmentReason())
                .notes(request.getNotes())
                .pochiAssignments(new HashSet<>(assignments))
                .build();

        try {
            return settlementMapper.toResponse(settlementRepository.saveAndFlush(settlement));
        } catch (DataIntegrityViolationException exception) {
            if (isAssignmentUniqueConstraintViolation(exception)) {
                throw new PochiAssignmentAlreadySettledException(requestedIds.getFirst());
            }
            throw exception;
        }
    }

    @Override
    public SettlementResponse findById(Integer id) {
        return settlementMapper.toResponse(findSettlement(id));
    }

    @Override
    public Page<SettlementResponse> findAll(
            Integer driverId,
            Integer pochiId,
            LocalDate workDate,
            LocalDate workDateFrom,
            LocalDate workDateTo,
            Pageable pageable) {
        if ((workDateFrom != null && workDateTo != null && workDateTo.isBefore(workDateFrom))
                || (workDate != null && (workDateFrom != null || workDateTo != null))) {
            throw new InvalidSettlementFilterException();
        }

        Specification<Settlement> specification = (root, query, builder) -> builder.conjunction();
        if (driverId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("driver").get("id"), driverId));
        }
        if (pochiId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("pochi").get("id"), pochiId));
        }
        if (workDate != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("workDate"), workDate));
        }
        if (workDateFrom != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("workDate"), workDateFrom));
        }
        if (workDateTo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("workDate"), workDateTo));
        }
        return settlementRepository.findAll(specification, pageable).map(settlementMapper::toResponse);
    }

    @Override
    @Transactional
    public SettlementResponse update(Integer id, SettlementUpdateRequest request) {
        Settlement settlement = findSettlement(id);
        validateNotCharged(settlement);
        validateRequestAmounts(request.getAcceptedAmount());
        validateAdjustment(request.getAcceptedAmount(), settlement.getExpectedAmount(),
                request.getAdjustmentReason());
        settlementMapper.updateEntity(request, settlement);
        return settlementMapper.toResponse(settlementRepository.save(settlement));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Settlement settlement = findSettlement(id);
        validateNotCharged(settlement);
        settlementRepository.delete(settlement);
    }

    private Settlement findSettlement(Integer id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException(id));
    }

    private void validateNotCharged(Settlement settlement) {
        if (chargeSettlementRepository.existsBySettlementId(settlement.getId())) {
            throw new AdministrationChargeConflictException(
                    "Settlement " + settlement.getId() + " is locked by an Administration Charge.");
        }
    }

    private List<PochiAssignment> resolveAssignments(List<Integer> requestedIds) {
        Map<Integer, PochiAssignment> assignmentsById = new HashMap<>();
        assignmentRepository.findAllById(requestedIds)
                .forEach(assignment -> assignmentsById.put(assignment.getId(), assignment));
        for (Integer id : requestedIds) {
            if (!assignmentsById.containsKey(id)) {
                throw new PochiAssignmentNotFoundException(id);
            }
        }
        return requestedIds.stream().map(assignmentsById::get).toList();
    }

    private void validateUniqueAssignmentIds(List<Integer> assignmentIds) {
        if (assignmentIds == null || assignmentIds.isEmpty() || assignmentIds.size() > 2
                || new HashSet<>(assignmentIds).size() != assignmentIds.size()
                || assignmentIds.stream().anyMatch(id -> id == null)) {
            throw new InvalidSettlementException("Select one or two distinct Pochi Assignments.");
        }
    }

    private void validateAssignmentsAvailable(List<Integer> assignmentIds) {
        Set<Integer> settledIds = settlementRepository.findSettledAssignmentIds(assignmentIds);
        for (Integer id : assignmentIds) {
            if (settledIds.contains(id)) {
                throw new PochiAssignmentAlreadySettledException(id);
            }
        }
    }

    private void validateCompatibleAssignments(List<PochiAssignment> assignments) {
        PochiAssignment first = assignments.getFirst();
        boolean incompatible = assignments.stream().anyMatch(assignment ->
                !assignment.getDriver().getId().equals(first.getDriver().getId())
                        || !assignment.getPochi().getId().equals(first.getPochi().getId())
                        || !assignment.getAssignmentDate().equals(first.getAssignmentDate()));
        boolean duplicateShift = assignments.stream()
                .map(PochiAssignment::getShift)
                .distinct()
                .count() != assignments.size();
        if (incompatible || duplicateShift) {
            throw new InvalidSettlementException(
                    "Pochi Assignments must have different shifts and the same driver, Pochi, and work date.");
        }
    }

    private void validateRequestAmounts(BigDecimal acceptedAmount) {
        if (acceptedAmount == null || acceptedAmount.signum() < 0) {
            throw new InvalidSettlementException("acceptedAmount must be greater than or equal to zero.");
        }
    }

    private void validateAdjustment(
            BigDecimal acceptedAmount, BigDecimal expectedAmount, String adjustmentReason) {
        if (expectedAmount != null && acceptedAmount.compareTo(expectedAmount) != 0
                && (adjustmentReason == null || adjustmentReason.isBlank())) {
            throw new InvalidSettlementException(
                    "adjustmentReason is required when acceptedAmount differs from expectedAmount.");
        }
    }

    private boolean isAssignmentUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
                return ASSIGNMENT_UNIQUE_CONSTRAINT.equals(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}

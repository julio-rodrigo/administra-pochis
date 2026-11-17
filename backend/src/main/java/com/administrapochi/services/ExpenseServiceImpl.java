package com.administrapochi.services;

import com.administrapochi.exceptions.ExpenseConflictException;
import com.administrapochi.exceptions.ExpenseNotFoundException;
import com.administrapochi.exceptions.ExpenseTypeConflictException;
import com.administrapochi.exceptions.ExpenseTypeNotFoundException;
import com.administrapochi.exceptions.InvalidExpenseException;
import com.administrapochi.exceptions.PochiAssignmentPochiInactiveException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.exceptions.UserNotFoundException;
import com.administrapochi.models.dtos.expenses.CancelExpenseRequest;
import com.administrapochi.models.dtos.expenses.CreateExpenseRequest;
import com.administrapochi.models.dtos.expenses.ExpenseResponse;
import com.administrapochi.models.dtos.expenses.UpdateExpenseRequest;
import com.administrapochi.models.entities.Expense;
import com.administrapochi.models.entities.ExpenseType;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.ExpenseStatus;
import com.administrapochi.models.mappers.ExpenseMapper;
import com.administrapochi.repositories.ExpenseRepository;
import com.administrapochi.repositories.ExpenseTypeRepository;
import com.administrapochi.repositories.PochiRepository;
import com.administrapochi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseServiceImpl implements ExpenseService {

    private static final Integer TEMPORARY_USER_ID = 2;

    private final ExpenseRepository expenseRepository;
    private final ExpenseTypeRepository expenseTypeRepository;
    private final PochiRepository pochiRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    @Override
    @Transactional
    public ExpenseResponse create(CreateExpenseRequest request) {
        validateRequest(request.getExpenseTypeId(), request.getAmount(), request.getExpenseDate());
        ExpenseType expenseType = findExpenseType(request.getExpenseTypeId());
        Pochi pochi = resolvePochi(expenseType, request.getPochiId());
        User temporaryUser = findTemporaryUser();
        LocalDateTime now = LocalDateTime.now();

        Expense expense = expenseMapper.toEntity(request);
        expense.setExpenseType(expenseType);
        expense.setPochi(pochi);
        expense.setPaidByUser(temporaryUser);
        expense.setRegisteredByUser(temporaryUser);
        expense.setDescription(normalizeNullableText(request.getDescription()));
        expense.setStatus(ExpenseStatus.ACTIVE);
        expense.setCreatedAt(now);
        expense.setUpdatedAt(now);
        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Override
    public ExpenseResponse findById(Integer id) {
        return expenseMapper.toResponse(findExpense(id));
    }

    @Override
    public Page<ExpenseResponse> findAll(
            Integer expenseTypeId,
            Integer pochiId,
            ExpenseStatus status,
            LocalDate expenseDateFrom,
            LocalDate expenseDateTo,
            Pageable pageable) {
        validateFilters(expenseTypeId, pochiId, expenseDateFrom, expenseDateTo);
        Specification<Expense> specification = (root, query, builder) -> builder.conjunction();
        if (expenseTypeId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("expenseType").get("id"), expenseTypeId));
        }
        if (pochiId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("pochi").get("id"), pochiId));
        }
        if (status != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), status));
        }
        if (expenseDateFrom != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("expenseDate"), expenseDateFrom));
        }
        if (expenseDateTo != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("expenseDate"), expenseDateTo));
        }
        return expenseRepository.findAll(specification, pageable).map(expenseMapper::toResponse);
    }

    @Override
    @Transactional
    public ExpenseResponse update(Integer id, UpdateExpenseRequest request) {
        Expense expense = findExpenseForUpdate(id);
        validateExpenseIsActive(expense);
        validateRequest(request.getExpenseTypeId(), request.getAmount(), request.getExpenseDate());
        // TODO: Apply financial closed-period validation when a reusable policy is introduced.
        ExpenseType expenseType = findExpenseType(request.getExpenseTypeId());
        Pochi pochi = resolvePochi(expenseType, request.getPochiId());

        expenseMapper.updateEntity(request, expense);
        expense.setExpenseType(expenseType);
        expense.setPochi(pochi);
        expense.setDescription(normalizeNullableText(expense.getDescription()));
        expense.setUpdatedAt(LocalDateTime.now());
        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Override
    @Transactional
    public ExpenseResponse cancel(Integer id, CancelExpenseRequest request) {
        if (request == null || request.getCancellationReason() == null
                || request.getCancellationReason().isBlank()) {
            throw new InvalidExpenseException("cancellationReason must not be blank.");
        }
        Expense expense = findExpenseForUpdate(id);
        validateExpenseIsActive(expense);
        // TODO: Apply financial closed-period validation when a reusable policy is introduced.
        User temporaryUser = findTemporaryUser();
        LocalDateTime now = LocalDateTime.now();

        expense.setStatus(ExpenseStatus.CANCELLED);
        expense.setCancellationReason(request.getCancellationReason().trim());
        expense.setCancelledAt(now);
        expense.setCancelledByUser(temporaryUser);
        expense.setUpdatedAt(now);
        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    private Expense findExpense(Integer id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    private Expense findExpenseForUpdate(Integer id) {
        return expenseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    private ExpenseType findExpenseType(Integer id) {
        ExpenseType expenseType = expenseTypeRepository.findById(id)
                .orElseThrow(() -> new ExpenseTypeNotFoundException(id));
        if (!Boolean.TRUE.equals(expenseType.getActive())) {
            throw new ExpenseTypeConflictException("Inactive Expense Types cannot be assigned to an Expense.");
        }
        return expenseType;
    }

    private Pochi resolvePochi(ExpenseType expenseType, Integer pochiId) {
        if (Boolean.TRUE.equals(expenseType.getRequiresPochi()) && pochiId == null) {
            throw new InvalidExpenseException("pochiId is required for this Expense Type.");
        }
        if (pochiId == null) {
            return null;
        }
        Pochi pochi = pochiRepository.findById(pochiId)
                .orElseThrow(() -> new PochiNotFoundException(pochiId));
        if (!Boolean.TRUE.equals(pochi.getActive())) {
            throw new PochiAssignmentPochiInactiveException(pochiId);
        }
        return pochi;
    }

    private User findTemporaryUser() {
        return userRepository.findById(TEMPORARY_USER_ID)
                .orElseThrow(() -> new UserNotFoundException(TEMPORARY_USER_ID));
    }

    private void validateRequest(Integer expenseTypeId, BigDecimal amount, LocalDate expenseDate) {
        if (expenseTypeId == null || expenseTypeId <= 0) {
            throw new InvalidExpenseException("expenseTypeId must be a positive integer.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2 || amount.precision() - amount.scale() > 10) {
            throw new InvalidExpenseException(
                    "amount must be greater than zero and have at most 10 integer and 2 decimal digits.");
        }
        if (expenseDate == null) {
            throw new InvalidExpenseException("expenseDate is required.");
        }
        if (expenseDate.isAfter(LocalDate.now())) {
            throw new InvalidExpenseException("expenseDate cannot be in the future.");
        }
    }

    private void validateFilters(
            Integer expenseTypeId, Integer pochiId, LocalDate from, LocalDate to) {
        if ((expenseTypeId != null && expenseTypeId <= 0)
                || (pochiId != null && pochiId <= 0)
                || (from != null && to != null && to.isBefore(from))) {
            throw new InvalidExpenseException("Expense filters contain invalid values.");
        }
    }

    private void validateExpenseIsActive(Expense expense) {
        if (expense.getStatus() != ExpenseStatus.ACTIVE) {
            throw new ExpenseConflictException("Cancelled Expenses cannot be updated or cancelled.");
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

package com.administrapochi.services;

import com.administrapochi.exceptions.ExpenseTypeConflictException;
import com.administrapochi.exceptions.ExpenseTypeNotFoundException;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeCreateRequest;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeResponse;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeUpdateRequest;
import com.administrapochi.models.entities.ExpenseType;
import com.administrapochi.models.mappers.ExpenseTypeMapper;
import com.administrapochi.repositories.ExpenseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseTypeServiceImpl implements ExpenseTypeService {

    private final ExpenseTypeRepository expenseTypeRepository;
    private final ExpenseTypeMapper expenseTypeMapper;

    @Override
    public List<ExpenseTypeResponse> findAll(Boolean active) {
        List<ExpenseType> expenseTypes = active == null
                ? expenseTypeRepository.findAll()
                : expenseTypeRepository.findByActive(active);
        return expenseTypeMapper.toResponseList(expenseTypes);
    }

    @Override
    public ExpenseTypeResponse findById(Integer id) {
        return expenseTypeMapper.toResponse(findExpenseType(id));
    }

    @Override
    @Transactional
    public ExpenseTypeResponse create(ExpenseTypeCreateRequest request) {
        String code = normalizeCode(request.getCode());
        String name = normalizeName(request.getName());
        validateUniqueCode(code);
        validateUniqueName(name);

        ExpenseType expenseType = expenseTypeMapper.toEntity(request);
        expenseType.setCode(code);
        expenseType.setName(name);
        expenseType.setDescription(normalizeDescription(request.getDescription()));
        if (expenseType.getRequiresPochi() == null) {
            expenseType.setRequiresPochi(true);
        }
        expenseType.setActive(true);

        return expenseTypeMapper.toResponse(expenseTypeRepository.save(expenseType));
    }

    @Override
    @Transactional
    public ExpenseTypeResponse update(Integer id, ExpenseTypeUpdateRequest request) {
        ExpenseType expenseType = findExpenseType(id);
        String code = request.getCode() == null ? null : normalizeCode(request.getCode());
        String name = request.getName() == null ? null : normalizeName(request.getName());
        if (code != null && expenseTypeRepository.existsByCodeAndIdNot(code, id)) {
            throw duplicateCode(code);
        }
        if (name != null && expenseTypeRepository.existsByNameAndIdNot(name, id)) {
            throw duplicateName(name);
        }

        expenseTypeMapper.updateEntity(request, expenseType);
        if (code != null) {
            expenseType.setCode(code);
        }
        if (name != null) {
            expenseType.setName(name);
        }
        if (request.getDescription() != null) {
            expenseType.setDescription(normalizeDescription(request.getDescription()));
        }

        return expenseTypeMapper.toResponse(expenseTypeRepository.save(expenseType));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        ExpenseType expenseType = findExpenseType(id);
        if (Boolean.TRUE.equals(expenseType.getActive())) {
            expenseType.setActive(false);
            expenseTypeRepository.save(expenseType);
        }
    }

    private ExpenseType findExpenseType(Integer id) {
        return expenseTypeRepository.findById(id)
                .orElseThrow(() -> new ExpenseTypeNotFoundException(id));
    }

    private void validateUniqueCode(String code) {
        if (expenseTypeRepository.existsByCode(code)) {
            throw duplicateCode(code);
        }
    }

    private void validateUniqueName(String name) {
        if (expenseTypeRepository.existsByName(name)) {
            throw duplicateName(name);
        }
    }

    private ExpenseTypeConflictException duplicateCode(String code) {
        return new ExpenseTypeConflictException("Expense Type code '" + code + "' already exists");
    }

    private ExpenseTypeConflictException duplicateName(String name) {
        return new ExpenseTypeConflictException("Expense Type name '" + name + "' already exists");
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

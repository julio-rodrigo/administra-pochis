package com.administrapochi.services;

import com.administrapochi.models.dtos.expensetypes.ExpenseTypeCreateRequest;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeResponse;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeUpdateRequest;

import java.util.List;

public interface ExpenseTypeService {

    List<ExpenseTypeResponse> findAll(Boolean active);

    ExpenseTypeResponse findById(Integer id);

    ExpenseTypeResponse create(ExpenseTypeCreateRequest request);

    ExpenseTypeResponse update(Integer id, ExpenseTypeUpdateRequest request);

    void delete(Integer id);
}

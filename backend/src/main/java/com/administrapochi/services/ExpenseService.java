package com.administrapochi.services;

import com.administrapochi.models.dtos.expenses.CancelExpenseRequest;
import com.administrapochi.models.dtos.expenses.CreateExpenseRequest;
import com.administrapochi.models.dtos.expenses.ExpenseResponse;
import com.administrapochi.models.dtos.expenses.UpdateExpenseRequest;
import com.administrapochi.models.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ExpenseService {

    ExpenseResponse create(CreateExpenseRequest request);

    ExpenseResponse findById(Integer id);

    Page<ExpenseResponse> findAll(
            Integer expenseTypeId,
            Integer pochiId,
            ExpenseStatus status,
            LocalDate expenseDateFrom,
            LocalDate expenseDateTo,
            Pageable pageable);

    ExpenseResponse update(Integer id, UpdateExpenseRequest request);

    ExpenseResponse cancel(Integer id, CancelExpenseRequest request);
}

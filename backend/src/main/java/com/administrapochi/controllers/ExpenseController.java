package com.administrapochi.controllers;

import com.administrapochi.models.dtos.expenses.CancelExpenseRequest;
import com.administrapochi.models.dtos.expenses.CreateExpenseRequest;
import com.administrapochi.models.dtos.expenses.ExpenseResponse;
import com.administrapochi.models.dtos.expenses.UpdateExpenseRequest;
import com.administrapochi.models.enums.ExpenseStatus;
import com.administrapochi.services.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody CreateExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(expenseService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> findAll(
            @RequestParam(required = false) Integer expenseTypeId,
            @RequestParam(required = false) Integer pochiId,
            @RequestParam(required = false) ExpenseStatus status,
            @RequestParam(required = false) LocalDate expenseDateFrom,
            @RequestParam(required = false) LocalDate expenseDateTo,
            @PageableDefault(size = 20, sort = {"expenseDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(expenseService.findAll(
                expenseTypeId, pochiId, status, expenseDateFrom, expenseDateTo, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable Integer id, @Valid @RequestBody UpdateExpenseRequest request) {
        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ExpenseResponse> cancel(
            @PathVariable Integer id, @Valid @RequestBody CancelExpenseRequest request) {
        return ResponseEntity.ok(expenseService.cancel(id, request));
    }
}
